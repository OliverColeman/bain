package com.ojcoleman.bain.neuron.rate;

import com.ojcoleman.bain.base.*;
import com.ojcoleman.bain.neuron.spiking.FixedFrequencyNeuronConfiguration;

/**
 * A base for (typically rate-based encoding type) neurons that include a bias in the activation. Sub-classes must add the {@link #bias} to the relevant
 * calculations, for example with <code>neuronInputs[neuronID] += bias[neuronID];</code> in the {@link #run()} method.
 * 
 * @author Oliver J. Coleman
 */
public abstract class NeuronCollectionWithBias<C extends NeuronConfiguration> extends NeuronCollection<C> {
	/**
	 * The bias value for each neuron.
	 */
	protected double[] bias;

	/**
	 * Flag to indicate if any of the bias values have been modified since the last step. This is used to determine if we need to put() the {@link #bias}
	 * array/buffer when using OpenCL.
	 */
	protected boolean biasModified;

	public void init() {
		super.init();
		if (bias == null || bias.length != size) {
			bias = new double[size];
		}
		put(bias);
	}

	/**
	 * Get the bias the given neuron.
	 */
	public double getBias(int neuronIndex) {
		return bias[neuronIndex];
	}

	/**
	 * Set the bias for the given neuron.
	 */
	public void setBias(int neuronIndex, double b) {
		bias[neuronIndex] = b;
		biasModified = true;
	}

	@Override
	public void step() {
		if (biasModified) {
			put(bias);
		}
		super.step();
	}
}