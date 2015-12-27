package com.ojcoleman.bain.base;

import java.util.Arrays;

/**
 * <p>
 * Base class for all synapse collections. A SynapseCollection is expected to be used in conjunction with a
 * {@link NeuronCollection}; The methods to set and get the pre- and post-synaptic neurons for a synapse reference the
 * index of neurons in the associated NeuronCollection.
 * </p>
 * <p>
 * Sub-classes must override the methods {@link #run()}, {@link #createCollection(int size)}
 * {@link #getConfigSingleton()}. Sub-classes will need to override the methods {@link #init()},{@link #reset()} and
 * {@link #ensureStateVariablesAreFresh()} if they use custom state variables. Sub-classes may wish/need to override the
 * methods: {@link #step()}, {@link #getStateVariableNames()}, {@link #getStateVariableValues(int)} and {@link #isNotUsed(int)}.
 * </p>
 * 
 * @author Oliver J. Coleman
 */
public abstract class SynapseCollection<C extends SynapseConfiguration> extends ConfigurableComponentCollection<C> {
	/**
	 * The current efficacy of each synapse.
	 */
	public double[] efficacy;

	/**
	 * The initial efficacy of each synapse. The {@link #efficacy} of each synapse is reset to this when
	 * {@link #reset()} is called. By default the initial efficacy for each synapse is 0, but will be set (along with
	 * {@link #efficacy}) when {@link #setEfficacy(int, double)} is called.
	 */
	public double[] initialEfficacy;

	/**
	 * A reference to the {@link com.ojcoleman.bain.base.NeuronCollection#outputs} from the associated NeuronCollection.
	 */
	protected double[] neuronOutputs;

	/**
	 * A reference to the {@link com.ojcoleman.bain.base.NeuronCollection#spikings} from the associated NeuronCollection.
	 */
	protected boolean[] neuronSpikings;

	/**
	 * A reference to the {@link com.ojcoleman.bain.base.NeuronCollection#inputs} from the associated NeuronCollection.
	 */
	protected double[] neuronInputs;

	/**
	 * Indexes of the pre-synaptic neurons for each synapse.
	 */
	protected int[] preIndexes;

	/**
	 * Indexes of the post-synaptic neurons for each synapse.
	 */
	protected int[] postIndexes;

	/**
	 * Flag to indicate if the pre- or post-synaptic connections have changed for any synapse. This is used to determine
	 * if we need to put() the {@link #preIndexes} and {@link #postIndexes} arrays/buffers when using OpenCL.
	 */
	protected boolean preOrPostIndexesModified;

	/**
	 * Flag to indicate if the efficacy values have been manually modified since the last simulation step.
	 */
	protected boolean efficaciesModified;

	@Override
	public void init() {
		super.init();
		if (efficacy == null || efficacy.length != size) {
			outputs = new double[size];
			initialEfficacy = new double[size];
			efficacy = new double[size];
			preIndexes = new int[size];
			postIndexes = new int[size];
		}
		if (network != null) {
			neuronOutputs = network.getNeurons().getOutputs();
			neuronInputs = network.getNeurons().getInputs();
			neuronSpikings = network.getNeurons().getSpikings();
		}

		// In case explicit mode is being used for the Aparapi kernel.
		put(outputs);
		outputsStale = false;
		put(efficacy);
		put(preIndexes);
		put(postIndexes);
		preOrPostIndexesModified = false;
		efficaciesModified = false;
	}

	/**
	 * Resets all synapses to their initial state (see {@link #initialEfficacy}). Sub-classes should override this
	 * method if state variables other than the efficacy and synapseOutputs must be reset, or if they should be set to
	 * something other than 0. The overriding method should invoke this super-method (before doing anything else).
	 * Arrays/buffers reset here and used in the run() method/kernel should be transferred to the execution hardware
	 * using put().
	 */
	@Override
	public void reset() {
		super.reset();
		System.arraycopy(initialEfficacy, 0, efficacy, 0, efficacy.length);
		efficaciesModified = true;

	}

	@Override
	public void step() {
		// At the moment Aparapi doesn't allow sharing buffers between kernels
		// or allow kernels with multiple entry points in a way that is
		// compatible with a framework such as this. Thus we must ensure that
		// fresh versions of the following buffers are available to this kernel
		// by "putting" them there.
		network.getNeurons().ensureInputsAreFresh();
		network.getNeurons().ensureOutputsAreFresh();
		put(neuronOutputs); // neuron outputs are used by many synapse models.
		put(neuronSpikings); // neuron spikings are used by many synapse models.
		put(neuronInputs); // neuron inputs are calculated in run(), and are typically reset to 0 by the neuron model in
							// the previous simulation step.
		if (preOrPostIndexesModified) {
			put(preIndexes);
			put(postIndexes);
		}
		if (efficaciesModified) {
			put(efficacy);
		}
		super.step();
		outputsStale = true;
		get(neuronInputs); // See note above.
	}

	/**
	 * Implements the basic infrastructure for processing a synapse by updating the values of {@link #outputs} and
	 * {@link #neuronInputs}. Sub-classes may override this method, and if they modify the {@link #efficacy} they must
	 * call the super-method <strong>after</strong> modifying it.
	 */
	@Override
	public void run() {
		int synapseID = this.getGlobalId();
		outputs[synapseID] = neuronOutputs[preIndexes[synapseID]] * efficacy[synapseID];
		neuronInputs[postIndexes[synapseID]] += outputs[synapseID];
	}

	@Override
	public double getInput(int index) {
		ensureInputsAreFresh();
		return neuronOutputs[preIndexes[index]];
	}

	/**
	 * {@inheritDoc} A SynapseCollection uses the outputs of pre-synaptic neurons as inputs, thus it does not provide a
	 * reference to an internal array. Instead an array is generated with an element for each synapse, and the relevant
	 * output values for each pre-synaptic neuron copied into it. Consider using {@link #getInputs(double[])}.
	 */
	@Override
	public double[] getInputs() {
		return getInputs(null);
	}

	/**
	 * Produces the same output as {@link #getInputs} but accepts an array to put the data in. If the the given array
	 * has length less than {@link #getSize()}, or is null, a new array is created.
	 */
	public double[] getInputs(double[] inputs) {
		ensureInputsAreFresh();
		if (inputs == null || inputs.length < size) {
			inputs = new double[size];
		}
		for (int s = 0; s < size; s++) {
			inputs[s] = neuronOutputs[preIndexes[s]];
		}
		return inputs;
	}

	/**
	 * SynapseCollection implements this as an empty method as it generally does not make sense to add external input to
	 * a synapse. The same effect can be achieved by adding to the output value of the pre-synaptic neuron.
	 */
	@Override
	public void addInput(int index, double input) {
	}

	@Override
	public void ensureInputsAreFresh() {
		network.getNeurons().ensureOutputsAreFresh();
		inputsStale = false;
	}

	/**
	 * Set the pre-synaptic neuron for a synapse.
	 * 
	 * @param synapseIndex The index of the synapse to set the pre-synaptic neuron for.
	 * @param neuronIndex The index of the pre-synaptic neuron in the NeuronCollection associated with this
	 *            SynapseCollection.
	 */
	public void setPreNeuron(int synapseIndex, int neuronIndex) {
		preIndexes[synapseIndex] = neuronIndex;
		preOrPostIndexesModified = true;
	}

	/**
	 * Get the pre-synaptic Neuron for a synapse.
	 * 
	 * @param synapseIndex The index of the synapse to get the pre-synaptic for.
	 * @return The index of the pre-synaptic in the NeuronCollection associated with this SynapseCollection.
	 */
	public int getPreNeuron(int synapseIndex) {
		return preIndexes[synapseIndex];
	}

	/**
	 * Set the post-synaptic Neuron for a synapse.
	 * 
	 * @param synapseIndex The index of the to set the post-synaptic for.
	 * @param neuronIndex The index of the post-synaptic in the NeuronCollection associated with this SynapseCollection.
	 */
	public void setPostNeuron(int synapseIndex, int neuronIndex) {
		postIndexes[synapseIndex] = neuronIndex;
		preOrPostIndexesModified = true;
	}

	/**
	 * Get the post-synaptic Neuron for a synapse.
	 * 
	 * @param synapseIndex The index of the to get the post-synaptic for.
	 * @return The index of the post-synaptic in the NeuronCollection associated with this SynapseCollection.
	 */
	public int getPostNeuron(int synapseIndex) {
		return postIndexes[synapseIndex];
	}

	/**
	 * Set the pre- and post-synaptic neurons for a synapse.
	 * 
	 * @param synapseIndex The index of the synapse to set the pre-synaptic neuron for.
	 * @param preNeuronIndex The index of the pre-synaptic neuron in the NeuronCollection associated with this
	 *            SynapseCollection.
	 * @param postNeuronIndex The index of the post-synaptic neuron in the NeuronCollection associated with this
	 *            SynapseCollection.
	 */
	public void setPreAndPostNeurons(int synapseIndex, int preNeuronIndex, int postNeuronIndex) {
		preIndexes[synapseIndex] = preNeuronIndex;
		postIndexes[synapseIndex] = postNeuronIndex;
		preOrPostIndexesModified = true;
	}

	/**
	 * Get current strength (weight) value.
	 * 
	 * @param synapseIndex The index of the synapse to get the efficacy of.
	 * @return the current strength (weight) value.
	 */
	public double getEfficacy(int synapseIndex) {
		// If efficacies have been modified then we've already pulled the latest values from the SIMD hardware.
		if (!efficaciesModified && stateVariablesStale) {
			get(efficacy);
		}
		return efficacy[synapseIndex];
	}
	
	/**
	 * Get initial strength (weight) value.
	 * 
	 * @param synapseIndex The index of the synapse to get the initial efficacy of.
	 * @return the initial strength (weight) value.
	 */
	public double getInitialEfficacy(int synapseIndex) {
		return initialEfficacy[synapseIndex];
	}

	/**
	 * Set current strength (weight) value. This will also set the corresponding value in {@link #initialEfficacy}.
	 * 
	 * @param synapseIndex The index of the synapse to set the efficacy of.
	 * @param newEfficacy The new efficacy value.
	 */
	public void setEfficacy(int synapseIndex, double newEfficacy) {
		// If efficacies have been modified then we've already pulled the latest values from the SIMD hardware
		// (and we don't want to overwrite values that have already been modified by previous calls to this method),
		// otherwise we need pull the latest values before we start setting individual ones as we're going to
		// be pushing back all the values during the next simulation step.
		if (!efficaciesModified && stateVariablesStale) {
			get(efficacy);
		}
		efficacy[synapseIndex] = newEfficacy;
		initialEfficacy[synapseIndex] = newEfficacy;
		stateVariablesStale = true;
		efficaciesModified = true;
	}

	/**
	 * Returns a reference to the internal efficacy array, to allow efficient getting and setting of efficacy values.
	 * <strong>If reading values from the returned array, this method must be called after every call to
	 * {@link com.ojcoleman.bain.NeuralNetwork#step()} or {@link com.ojcoleman.bain.NeuralNetwork#run(int)}.</strong>
	 * This will ensure that the current values are retrieved from the SIMD hardware (eg GPU) if necessary. <strong>If
	 * setting values in the returned array the method {@link #setEfficaciesModified()} must be called.</strong>. This
	 * will ensure that the modified values are pushed to the SIMD hardware if necessary during the next simulation
	 * step.
	 */
	public double[] getEfficacies() {
		// If efficacies have been modified then we've already pulled the latest values from the SIMD hardware
		// (and we don't want to overwrite values that have already been set), otherwise we need pull the latest values.
		if (!efficaciesModified && stateVariablesStale) {
			get(efficacy);
		}
		return efficacy;
	}

	/**
	 * If setting values in the array returned by {@link #getEfficacies()} this method must be called. This will ensure
	 * that the modified values are pushed to the SIMD hardware if necessary during the next simulation step and will
	 * update the values in {@link SynapseCollection#initialEfficacy}.
	 */
	public void setEfficaciesModified() {
		efficaciesModified = true;
		System.arraycopy(efficacy, 0, initialEfficacy, 0, efficacy.length);
	}

	@Override
	public SynapseConfiguration getComponentConfiguration(int componentIndex) {
		return configs.get(componentConfigIndexes[componentIndex]);
	}

	/**
	 * Return true iff this synapse has no effect on the network. This default implementation returns true iff the
	 * initial efficacy of the synapse is 0. Subclasses should override this method if some additional criteria should
	 * be used to determine this (for example a learning rate parameter for a synaptic plasticity function).
	 * @see #compress()
	 */
	public boolean isNotUsed(int synapseIndex) {
		return initialEfficacy[synapseIndex] == 0;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

	@Override
	public void ensureStateVariablesAreFresh() {
		if (stateVariablesStale) {
			getEfficacies();
			stateVariablesStale = false;
		}
	}

	/**
	 * {@inheritDoc} This default implementation returns the {@link #efficacy} field.
	 */
	@Override
	public String[] getStateVariableNames() {
		String[] names = { "efficacy" };
		return names;
	}

	/**
	 * {@inheritDoc} This default implementation returns the {@link #efficacy} field.
	 */
	@Override
	public double[] getStateVariableValues(int synapseIndex) {
		ensureStateVariablesAreFresh();
		double[] values = { efficacy[synapseIndex] };
		return values;
	}

	/**
	 * Remove synapses that are not in use (see {@link #isNotUsed(int)}) and reconfigure the
	 * SynapseCollection to move these towards the end of the collection, then sets the "populated" size 
	 * (see {@link ComponentCollection#setSizePopulated(int)}) of the collection accordingly so that
	 * calculations for the unused synapses are not performed. {@link #init()} is called at the end to
	 * ensure that configuration data is updated in sub-classes.
	 */
	public void compress() {
		init(); // Make sure config arrays are up to date with config objects so that isNotUsed() returns correct result.
		int current = 0;
		int end = efficacy.length - 1;
		
		// Swap unused synapses to the end of the collection.
		while (current != end) {
			if (isNotUsed(current)) {
				swap(outputs, current, end);
				swap(componentConfigIndexes, current, end);
				swap(efficacy, current, end);
				swap(initialEfficacy, current, end);
				swap(preIndexes, current, end);
				swap(postIndexes, current, end);
				// Don't increment current as we haven't checked the one for end that we've swapped current for.
				// This way we check the one we swapped current for in the next iteration.
				end--;
			}
			else {
				current++;
			}
		}
		// If we finished with a swap, then we haven't checked the synapse that end points to.
		if (isNotUsed(end)) end--;
		
		// Populated size is index of last useful synapse plus one.
		setSizePopulated(end+1);
		init(); // Make sure changes are pushed to GPU if necessary.
	}
	private void swap(double[] a, int x, int y) {
		double t = a[x];
		a[x] = a[y];
		a[y] = t;
	}
	private void swap(int[] a, int x, int y) {
		int t = a[x];
		a[x] = a[y];
		a[y] = t;
	}
}
