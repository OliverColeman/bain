package ojc.bain.base;

import java.util.Arrays;

import ojc.bain.base.*;

/**
 * <p>
 * Base class for all collections neurons. Sub-classes should update the values of the {@link #neuronOutputs neuronOutputs}
 * array when the {@link #step()} method is invoked. See {@link ComponentCollection} for details on implementing the step() and
 * run() methods.
 * </p>
 * <p>
 * Sub-classes must override the methods {@link #run()}, {@link #createCollection(int size)} {@link #getConfigSingleton()}. 
 * Sub-classes will need to override the methods {@link #init()},{@link #reset()} and {@link #ensureStateVariablesAreFresh()} if they use custom state variables.  
 * Sub-classes may wish/need to override the methods: {@link #step()},{@link #getStateVariableNames()} and {@link #getStateVariableValues(int)}.
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
	 * The current output values of the neurons.
	 */
	protected double[] neuronOutputs; // NOTE: Must have this name to allow sharing buffers in Aparapi kernel with
										// SynapseCollection.

	/**
	 * Boolean values to indicate whether a neuron spiked in the last time step.
	 */
	protected byte[] neuronSpikings; // NOTE: Must have this name to allow sharing buffers in Aparapi kernel with
										// SynapseCollection.

	/**
	 * The current input values of the neurons. Input comprises external input and input via synapses.
	 */
	protected double[] neuronInputs; // NOTE: Must have this name to allow sharing buffers in Aparapi kernel with
										// SynapseCollection.

	@Override
	public void init() {
		super.init();
		if (neuronOutputs == null || neuronOutputs.length != size) {
			neuronOutputs = new double[size];
			neuronSpikings = new byte[size];
		}
		put(neuronOutputs); // In case explicit mode is being used for the Aparapi kernel.
		put(neuronSpikings);
		outputsStale = false;
	}

	/**
	 * Returns true iff the specified spiked in the last time step.
	 */
	public boolean spiked(int index) {
		ensureOutputsAreFresh();
		return neuronSpikings[index] == 1;
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

	/**
	 * Returns the underlying array of spikings. This method is provided for efficiency reasons, the values of the array should
	 * not be altered. The values in the array returned by this method may become stale if the step() method is invoked
	 * subsequently; to get fresh values this method should be invoked again (this will return the same array but will also
	 * ensure that the values in the array are up to date by invoking ensureOutputsAreFresh()).
	 */
	public byte[] getSpikings() {
		return neuronSpikings;
	}

	@Override
	public double getInput(int index) {
		return neuronInputs[index];
	}

	@Override
	public double[] getInputs() {
		return neuronInputs;
	}
	
	@Override
	public void addInput(int index, double input) {
		neuronInputs[index] += input;
	}

	/**
	 * {@inheritDoc} The implementation of this method in NeuronCollection resets the values for {@link #neuronOutputs},
	 * {@link #neuronSpikings} and {@link #neuronInputs} to 0. If any of these values should be set to something other than 0,
	 * then this method should be overridden, and this super-method called from the overriding method before setting the values
	 * to the correct value and calling put([modified array]) with the relevant array.
	 */
	@Override
	public void reset() {
		super.reset();
		Arrays.fill(neuronOutputs, 0);
		Arrays.fill(neuronSpikings, (byte) 0);
		Arrays.fill(neuronInputs, 0);
		put(neuronOutputs); // In case explicit mode is being used for the Aparapi kernel.
		put(neuronSpikings); // In case explicit mode is being used for the Aparapi kernel.
		put(neuronInputs); // In case explicit mode is being used for the Aparapi kernel.
	}

	@Override
	public void step() {
		// get() and put() not necessary, using shared buffer between and kernels (perhaps in future versions we'll allow
		// kernels to run on disparate platforms).
		// put(synapseOutputs);
		super.step();
		// get(neuronOutputs);
	}

	/**
	 * Implements the basic infrastructure for processing a neuron by resetting the value of {@link #neuronInputs} and setting
	 * the value for {@link #neuronSpikings}. Sub-classes must override this method and call the super-method
	 * <strong>after</strong> they have made use of the value in neuronInputs. Alternatively, if neuronInputs should be reset to
	 * something other than 0 or a neuron spike is represented by something other than a value of 1 in neuronOutputs, then this
	 * super-method need not be called and the overriding method should instead reset the values for neuronInputs and set the
	 * value for neuronSpikings.
	 * 
	 */
	@Override
	public void run() {
		int neuronID = this.getGlobalId();
		neuronInputs[neuronID] = 0;
		neuronSpikings[neuronID] = (byte) (neuronOutputs[neuronID] >= 1 ? 1 : 0);
	}

	@Override
	public void ensureOutputsAreFresh() {
		if (outputsStale) {
			get(neuronOutputs);
			get(neuronSpikings);
			outputsStale = false;
		}
	}

	@Override
	public void ensureInputsAreFresh() {
		if (inputsStale) {
			get(neuronInputs);
			inputsStale = false;
		}
	}
}