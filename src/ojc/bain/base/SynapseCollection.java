package ojc.bain.base;

import java.util.Arrays;

/**
 * <p>
 * Base class for all synapse collections. A SynapseCollection is expected to be used in conjunction with a {@link NeuronCollection}; The methods to set and get
 * the pre- and post-synaptic neurons for a synapse reference the index of neurons in the associated NeuronCollection.
 * </p>
 * <p>
 * Sub-classes must override the methods {@link #run()}, {@link #createCollection(int size)} {@link #getConfigSingleton()}. Sub-classes will need to override
 * the methods {@link #init()},{@link #reset()} and {@link #ensureStateVariablesAreFresh()} if they use custom state variables. Sub-classes may wish/need to
 * override the methods: {@link #step()}, {@link #getStateVariableNames()} and {@link #getStateVariableValues(int)}.
 * </p>
 * 
 * @author Oliver J. Coleman
 */
public abstract class SynapseCollection<C extends SynapseConfiguration> extends ConfigurableComponentCollection<C> {
	/**
	 * The current efficacy of each synapse.
	 */
	protected double[] efficacy;

	/**
	 * The current output of each synapse.
	 */
	protected double[] synapseOutputs;

	/**
	 * The {@link ojc.bain.base.NeuronCollection#neuronOutputs} from the associated NeuronCollection.
	 */
	protected double[] neuronOutputs;

	/**
	 * The {@link ojc.bain.base.NeuronCollection#neuronSpikings} from the associated NeuronCollection.
	 */
	protected boolean[] neuronSpikings;

	/**
	 * The {@link ojc.bain.base.NeuronCollection#neuronInputs} from the associated NeuronCollection.
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
	 * Flag to indicate if the pre- or post-synaptic connections have changed for any synapse. This is used to determine if we need to put() the
	 * {@link #preIndexes} and {@link #postIndexes} arrays/buffers when using OpenCL.
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
			synapseOutputs = new double[size];
			efficacy = new double[size];
			preIndexes = new int[size];
			postIndexes = new int[size];

			// In case explicit mode is being used for the Aparapi kernel.
			put(synapseOutputs);
			outputsStale = false;
			put(efficacy);
			put(preIndexes);
			put(postIndexes);
			preOrPostIndexesModified = false;
			efficaciesModified = false;
		}
		if (simulation != null) {
			neuronOutputs = simulation.getNeurons().getOutputs();
			neuronInputs = simulation.getNeurons().getInputs();
			neuronSpikings = simulation.getNeurons().getSpikings();

			put(neuronOutputs);
			put(neuronInputs);
			put(neuronSpikings);
		}
	}

	/**
	 * Resets all synapses to their initial state. Sub-classes should override this method if state variables other than the efficacy and synapseOutputs must be
	 * reset, or if they should be set to something other than 0. The overriding method should invoke this super-method (before doing anything else).
	 * Arrays/buffers reset here and used in the run() method/kernel should be transferred to the execution hardware using put().
	 */
	@Override
	public void reset() {
		super.reset();
		Arrays.fill(efficacy, 0);
		Arrays.fill(synapseOutputs, 0);
		put(efficacy); // In case explicit mode is being used for the Aparapi kernel.
		put(synapseOutputs);
		efficaciesModified = false;
	}

	@Override
	public void step() {
		// put(neuronOutputs);
		put(neuronSpikings);
		put(neuronInputs);
		if (preOrPostIndexesModified) {
			put(preIndexes);
			put(postIndexes);
		}
		if (efficaciesModified) {
			put(efficacy);
		}
		super.step();
		get(neuronInputs);
	}

	/**
	 * Implements the basic infrastructure for processing a synapse by updating the values of {@link #synapseOutputs} and {@link #neuronInputs}. Sub-classes
	 * must override this method and call the super-method <strong>after</strong> they have updated {@link #efficacy}.
	 */
	@Override
	public void run() {
		int synapseID = this.getGlobalId();
		synapseOutputs[synapseID] = neuronOutputs[preIndexes[synapseID]] * efficacy[synapseID];
		neuronInputs[postIndexes[synapseID]] += synapseOutputs[synapseID];
	}

	@Override
	public double getOutput(int index) {
		ensureOutputsAreFresh();
		return synapseOutputs[index];
	}

	@Override
	public double[] getOutputs() {
		ensureOutputsAreFresh();
		return synapseOutputs;
	}

	@Override
	public double getInput(int index) {
		ensureInputsAreFresh();
		return neuronOutputs[preIndexes[index]];
	}

	/**
	 * {@inheritDoc} A SynapseCollection uses the outputs of pre-synaptic neurons as inputs, thus it does not provide a reference to an internal array. Instead
	 * an array is generated with an element for each neuron, and the output values for each pre-synaptic neuron copied into it. Consider using
	 * {@link #getInputs(double[])}.
	 */
	@Override
	public double[] getInputs() {
		return getInputs(null);
	}

	/**
	 * Produces the same output as {@link #getInputs} but accepts an array to put the data in. If the the given array has length less than {@link #getSize()},
	 * or is null, a new array is created.
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
	 * SynapseCollection implements this as an empty method as it generally does not make sense to add external input to a synapse. Sub-classes may override
	 * this method if it is necessary for them to supply input other than from a pre-synaptic neuron.
	 */
	@Override
	public void addInput(int index, double input) {
	}

	@Override
	public void ensureOutputsAreFresh() {
		if (outputsStale) {
			get(synapseOutputs);
			outputsStale = false;
		}
	}

	@Override
	public void ensureInputsAreFresh() {
		if (inputsStale) {
			get(neuronOutputs);
			inputsStale = false;
		}
	}

	/**
	 * Set the pre-synaptic neuron for a synapse.
	 * 
	 * @param synapseIndex The index of the synapse to set the pre-synaptic neuron for.
	 * @param neuronIndex The index of the pre-synaptic neuron in the NeuronCollection associated with this SynapseCollection.
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
	 * Set the post-synaptic for a .
	 * 
	 * @param synapseIndex The index of the to set the post-synaptic for.
	 * @param neuronIndex The index of the post-synaptic in the NeuronCollection associated with this SynapseCollection.
	 */
	public void setPostNeuron(int synapseIndex, int neuronIndex) {
		postIndexes[synapseIndex] = neuronIndex;
		preOrPostIndexesModified = true;
	}

	/**
	 * Get the post-synaptic Neuron for a .
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
	 * @param preNeuronIndex The index of the pre-synaptic neuron in the NeuronCollection associated with this SynapseCollection.
	 * @param postNeuronIndex The index of the post-synaptic neuron in the NeuronCollection associated with this SynapseCollection.
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
	 * Set current strength (weight) value.
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
		stateVariablesStale = true;
	}

	/**
	 * Returns a reference to the internal efficacy array, to allow efficient getting and setting of efficacy values. <strong>If reading values from the
	 * returned array, this method must be called after every call to {@link ojc.bain.NeuralNetwork#step()} or {@link ojc.bain.NeuralNetwork#run(int)}.</strong>
	 * This will ensure that the current values are retrieved from the SIMD hardware (eg GPU) if necessary. <strong>If setting values in the returned array the
	 * method {@link #setEfficaciesModified()} must be called.</strong>. This will ensure that the modified values are pushed to the SIMD hardware if necessary
	 * during the next simulation step.
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
	 * If setting values in the array returned by {@link #getEfficacies()} this method must be called. This will ensure that the modified values are pushed to the SIMD hardware if necessary
	 * during the next simulation step.
	 */
	public void setEfficaciesModified() {
		efficaciesModified = true;
	}

	@Override
	public SynapseConfiguration getComponentConfiguration(int componentIndex) {
		return configs.get(componentConfigIndexes[componentIndex]);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
}
