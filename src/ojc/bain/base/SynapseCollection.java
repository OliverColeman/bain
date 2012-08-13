package ojc.bain.base;

import java.util.Arrays;

import ojc.bain.base.*;
import ojc.bain.neuron.*;

/**
 * <p>
 * Base class for all ojc.bain.synapse collections. A SynapseCollection is expected to be used in conjunction with a
 * {@link NeuronCollection}; The methods to set and get the pre- and post-synaptic neurons for a ojc.bain.synapse reference the
 * index of neurons in the associated NeuronCollection.
 * </p>
 * 
 * <p>
 * The overridden run() method in a super-class should update the values in the {@link #efficacy} array as well as the
 * {@link #synapseOutputs} array like so: synapseOutputs[synapseID] = neuronOutputs[preIndexes[synapseID]] *
 * efficacy[synapseID]. The values in the synapseOutputs array are used to provide input to the neurons in the next iteration of
 * the simulation.
 * </p>
 * 
 * <p>
 * Unless extra processing, such as transferring additional arrays/buffers to/from a kernel executing on remote hardware (eg
 * GPU), is required, sub-classes need not override the {@link #step()} method. The overridden step() provided in this class
 * ensures that the (non-stale) {@link #neuronOutputs} are made available to the kernel, and retrieves the
 * {@link #synapseOutputs} if/when necessary.
 * 
 * @author Oliver J. Coleman
 */
public abstract class SynapseCollection<C extends ComponentConfiguration> extends ConfigurableComponentCollection<C> {
	/**
	 * The current efficacy of each ojc.bain.synapse. Range should be [-1, 1]
	 */
	protected double[] efficacy;

	/**
	 * The current output of each ojc.bain.synapse. Range should be [-1, 1]
	 */
	protected double[] synapseOutputs; // NOTE: Must have this name to allow sharing buffers in Aparapi kernel.

	/**
	 * The ojc.bain.neuron outputs from the associated NeuronCollection.
	 */
	protected double[] neuronOutputs;// NOTE: Must have this name to allow sharing buffers in Aparapi kernel.

	/**
	 * Indexes of the pre and post synaptic neurons for each ojc.bain.synapse.
	 */
	protected int[] preIndexes, postIndexes;

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
		}
		if (simulation != null) {
			neuronOutputs = simulation.getNeurons().getOutputs();
		}
	}

	/**
	 * Resets all synapses to their initial state. Sub-classes should override this method if state variables other than the
	 * efficacy must be reset, or if the efficacy should be set to something other than 0. The overriding method should invoke
	 * this super-method (before doing anything else). Arrays/buffers reset here and used in the run() method/kernel should be
	 * transferred to the execution hardware using put().
	 */
	@Override
	public void reset() {
		super.reset();
		Arrays.fill(efficacy, 0);
		put(efficacy); // In case explicit mode is being used for the Aparapi kernel.
	}

	@Override
	public void step() {
		// put(neuronOutputs); // Not necessary, using shared buffer between ojc.bain.neuron and ojc.bain.synapse kernels.
		super.step();
		// get(synapseOutputs); // Not necessary, using shared buffer between ojc.bain.neuron and ojc.bain.synapse kernels.
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
	public void ensureOutputsAreFresh() {
		if (outputsStale) {
			get(synapseOutputs);
			outputsStale = false;
		}
	}

	/**
	 * Set the pre-synaptic ojc.bain.neuron for a ojc.bain.synapse.
	 * 
	 * @param synapseIndex The index of the ojc.bain.synapse to set the pre-synaptic ojc.bain.neuron for.
	 * @param neuronIndex The index of the pre-synaptic ojc.bain.neuron in the NeuronCollection associated with this
	 *            SynapseCollection.
	 */
	public void setPreNeuron(int synapseIndex, int neuronIndex) {
		preIndexes[synapseIndex] = neuronIndex;
		put(preIndexes); // In case explicit mode is being used for the Aparapi kernel.
	}

	/**
	 * Get the pre-synaptic Neuron for a ojc.bain.synapse.
	 * 
	 * @param synapseIndex The index of the ojc.bain.synapse to get the pre-synaptic ojc.bain.neuron for.
	 * @return The index of the pre-synaptic ojc.bain.neuron in the NeuronCollection associated with this SynapseCollection.
	 */
	public int getPreNeuron(int synapseIndex) {
		return preIndexes[synapseIndex];
	}

	/**
	 * Set the post-synaptic ojc.bain.neuron for a ojc.bain.synapse.
	 * 
	 * @param synapseIndex The index of the ojc.bain.synapse to set the post-synaptic ojc.bain.neuron for.
	 * @param neuronIndex The index of the post-synaptic ojc.bain.neuron in the NeuronCollection associated with this
	 *            SynapseCollection.
	 */
	public void setPostNeuron(int synapseIndex, int neuronIndex) {
		postIndexes[synapseIndex] = neuronIndex;
		put(postIndexes); // In case explicit mode is being used for the Aparapi kernel.
	}

	/**
	 * Get the post-synaptic Neuron for a ojc.bain.synapse.
	 * 
	 * @param synapseIndex The index of the ojc.bain.synapse to get the post-synaptic ojc.bain.neuron for.
	 * @return The index of the post-synaptic ojc.bain.neuron in the NeuronCollection associated with this SynapseCollection.
	 */
	public int getPostNeuron(int synapseIndex) {
		return postIndexes[synapseIndex];
	}

	/**
	 * Get current strength (weight) value.
	 * 
	 * @return the current strength (weight) value.
	 */
	public double getEfficacy(int synapseIndex) {
		if (stateVariablesStale) {
			get(efficacy);
		}
		return efficacy[synapseIndex];
	}
}
