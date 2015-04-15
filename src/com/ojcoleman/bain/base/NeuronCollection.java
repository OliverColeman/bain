package com.ojcoleman.bain.base;

import java.util.Arrays;

/**
 * <p>
 * Base class for all collections of neurons. Sub-classes must override the methods {@link #run()}, {@link #createCollection(int size)}
 * {@link #getConfigSingleton()}. Sub-classes will need to override the methods {@link #init()},{@link #reset()} and {@link #ensureStateVariablesAreFresh()} if
 * they use custom state variables. Sub-classes may wish/need to override the methods: {@link #step()}, {@link #getStateVariableNames()} and
 * {@link #getStateVariableValues(int)}.
 * </p>
 * <p>
 * There is no explicit demarcation of input and output neurons. To provide input to the network the output values of neurons should be set to the
 * input values required (and the remaining output values not modified), either via {@link #getOutputs()} and {@link #setOutputsModified()} or
 * {@link #setOutput(int, double)}. To retrieve output values the methods {@link #getOutputs()} or {@link #getOutput(int)} should be used.
 * </p>
 * 
 * @author Oliver J. Coleman
 */
public abstract class NeuronCollection<C extends NeuronConfiguration> extends ConfigurableComponentCollection<C> {
	/**
	 * Refers to a pre-synaptic Neuron or spike.
	 */
	public static final int PRE = 0;

	/**
	 * Refers to a post-synaptic Neuron or spike.
	 */
	public static final int POST = 1;

	/**
	 * Boolean values to indicate whether a neuron spiked in the last time step.
	 */
	protected boolean[] spikings;

	/**
	 * The current input values of the neurons. Input comprises external input and input via synapses.
	 */
	protected double[] inputs;

	/**
	 * Flag to indicate if any of the inputs to the neurons have been modified. This is used to determine if we need to put() the {@link #inputs} array/buffer
	 * when using OpenCL.
	 */
	protected boolean inputsModified;

	@Override
	public void init() {
		super.init();
		if (outputs == null || outputs.length != size) {
			outputs = new double[size];
			spikings = new boolean[size];
			inputs = new double[size];
		}
		put(outputs); // In case explicit mode is being used for the Aparapi kernel.
		put(spikings);
		put(inputs);
		outputsStale = false;
		inputsStale = false;
		inputsModified = false;
	}

	/**
	 * Returns true iff the specified spiked in the last time step.
	 */
	public boolean spiked(int index) {
		ensureOutputsAreFresh();
		return spikings[index];
	}

	/**
	 * Returns the underlying array of spikings. This method is provided for efficiency reasons, the values of the array should not be altered. The values in
	 * the array returned by this method may become stale if the step() method is invoked subsequently; to get fresh values this method should be invoked again
	 * (this will return the same array but will also ensure that the values in the array are up to date by invoking ensureOutputsAreFresh()).
	 */
	public boolean[] getSpikings() {
		return spikings;
	}

	@Override
	public double getInput(int index) {
		ensureInputsAreFresh();
		return inputs[index];
	}

	@Override
	public double[] getInputs() {
		ensureInputsAreFresh();
		return inputs;
	}

	@Override
	public void addInput(int index, double input) {
		ensureInputsAreFresh();
		inputs[index] += input;
		inputsModified = true;
	}

	/**
	 * {@inheritDoc} The implementation of this method in NeuronCollection resets the values for {@link #outputs}, {@link #spikings} and {@link #inputs} to 0.
	 * If any of these values should be set to something other than 0, then this method should be overridden, and this super-method called from the overriding
	 * method before setting the values to the correct value and calling put([modified array]) with the relevant array.
	 */
	@Override
	public void reset() {
		super.reset();
		Arrays.fill(spikings, false);
		Arrays.fill(inputs, 0);
		inputsStale = false;
		inputsModified = true;
	}

	@Override
	public void step() {
		// At the moment Aparapi doesn't allow sharing buffers between kernels
		// or allow kernels with multiple entry points in a way that is
		// compatible with a framework such as this. Thus we must ensure that
		// fresh versions of the following buffers are available to this kernel
		// by "putting" them there.
		put(inputs); // neuron inputs are calculated by synapse model in previous simulation step.
		//if (inputsModified) {
		//  put(neuronInputs);
		//}

		super.step();

		outputsStale = true;
		inputsStale = true;
	}

	/**
	 * Implements the basic infrastructure for processing a neuron by resetting the value of {@link #inputs} and setting the value for {@link #spikings}.
	 * Sub-classes must override this method and call the super-method <strong>after</strong> they have made use of the value in {@link #inputs} to generate a
	 * value for {@link #outputs}. Alternatively, if {@link #inputs} should be reset to something other than 0 or a neuron spike is represented by something
	 * other than a value greater than 0 in {@link #outputs}, then this super-method need not be called and the overriding method should instead reset the
	 * values for {@link #inputs} AND set the value for {@link #spikings}.
	 */
	@Override
	public void run() {
		int neuronID = this.getGlobalId();
		inputs[neuronID] = 0;
		spikings[neuronID] = outputs[neuronID] > 0;
	}

	@Override
	public void ensureOutputsAreFresh() {
		if (outputsStale) {
			get(spikings);
		}
		super.ensureOutputsAreFresh();
	}

	@Override
	public void ensureInputsAreFresh() {
		if (inputsStale) {
			get(inputs);
			inputsStale = false;
			inputsModified = false;
		}
	}

	@Override
	public NeuronConfiguration getComponentConfiguration(int componentIndex) {
		return configs.get(componentConfigIndexes[componentIndex]);
	}
}