package ojc.bain.base;

import java.util.Arrays;

import ojc.bain.base.*;

/**
 * <p>
 * Base class for all collections neurons. Sub-classes should update the values of the {@link #neuronOutputs neuronOutputs}
 * array when the {@link #step()} method is invoked. See {@link ComponentCollection} for details on implementing the step() and
 * run() methods.
 * </p>
 * 
 * @author Oliver J. Coleman
 */
public abstract class NeuronCollection<C extends ComponentConfiguration> extends ConfigurableComponentCollection<C> {
	/**
	 * Refers to a pre-synaptic Neuron or spike.
	 */
	public static final int PRE = 0;

	/**
	 * Refers to a post-synaptic Neuron or spike.
	 */
	public static final int POST = 1;

	/**
	 * The current output values of the neurons. Range should be [0, 1].
	 */
	protected double[] neuronOutputs; // NOTE: Must have this name to allow sharing buffers in Aparapi kernel.

	/**
	 * The ojc.bain.synapse outputs from the associated SynapseCollection.
	 */
	protected double[] synapseOutputs; // NOTE: Must have this name to allow sharing buffers in Aparapi kernel.

	@Override
	public void init() {
		super.init();
		if (neuronOutputs == null || neuronOutputs.length != size) {
			neuronOutputs = new double[size];
		}
		put(neuronOutputs); // In case explicit mode is being used for the Aparapi kernel.
		outputsStale = false;

		if (simulation != null) {
			synapseOutputs = simulation.getSynapses().getOutputs();
		}
	}

	/**
	 * Returns true iff the specified ojc.bain.neuron spiked in the last time step. The default implementation of this method
	 * returns true iff the output for the specified ojc.bain.neuron is >= 1. Sub-classes may override this method if spiking is
	 * represented by some other condition.
	 */
	public boolean spiked(int index) {
		ensureOutputsAreFresh();
		return neuronOutputs[index] >= 1;
	}

	@Override
	public double getOutput(int index) {
		ensureOutputsAreFresh();
		return neuronOutputs[index];
	}

	@Override
	public double[] getOutputs() {
		ensureOutputsAreFresh();
		return neuronOutputs;
	}

	@Override
	public void reset() {
		super.reset();
		Arrays.fill(neuronOutputs, 0);
		put(neuronOutputs); // In case explicit mode is being used for the Aparapi kernel.
	}

	@Override
	public void step() {
		// put(synapseOutputs); // Not necessary, using shared buffer between ojc.bain.neuron and ojc.bain.synapse kernels.
		super.step();
		// get(neuronOutputs); // Not necessary, using shared buffer between ojc.bain.neuron and ojc.bain.synapse kernels.
	}

	@Override
	public void ensureOutputsAreFresh() {
		if (outputsStale) {
			get(neuronOutputs);
			outputsStale = false;
		}
	}
}