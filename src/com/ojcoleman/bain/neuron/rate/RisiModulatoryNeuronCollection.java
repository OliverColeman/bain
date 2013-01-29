package com.ojcoleman.bain.neuron.rate;

import java.util.Arrays;

import com.ojcoleman.bain.base.*;


/**
 * Implements the neuron model described by S. Risi, K.O. Stanley (2012) A Uniﬁed Approach to Evolving Plasticity and Neural Geometry.
 * 
 * This model must be used in combination with {@link com.ojcoleman.bain.synapse.rate.RisiModulatorySynapseCollection}
 * 
 * @author Oliver J. Coleman
 */
public class RisiModulatoryNeuronCollection extends SigmoidBipolarNeuronCollection {
	/**
	 * The current synaptic plasticity modulation inputs for the neurons.
	 */
	protected double[] modInputs;
	
	/**
	 * The bias value for synaptic plasticity modulation for each neuron.
	 */
	protected double[] modBias;
	
	/**
	 * The current synaptic plasticity modulation activation level for the neurons.
	 * This is used as a factor in determining the current plasticity of the incoming synapses.
	 */
	protected double[] modActivations;
	
	/**
	 * Flag to indicate if any of the modulatory bias values have been modified since the last step. This is used to determine if we need to put() the {@link #bias}
	 * array/buffer when using OpenCL.
	 */
	protected boolean modBiasModified;
	

	/**
	 * Create an empty RisiModulatorySigmoidNeuronCollection. This is used for retrieving a singleton to create a non-empty collection with.
	 */
	public RisiModulatoryNeuronCollection() {
		init();
	}

	/**
	 * Create a RisiModulatorySigmoidNeuronCollection.
	 * 
	 * @param size The size of this collection.
	 */
	public RisiModulatoryNeuronCollection(int size) {
		this.size = size;
		init();
	}

	@Override
	public void init() {
		super.init();
		
		if (modInputs == null || modInputs.length != size) {
			modInputs = new double[size];
			modBias = new double[size];
			modActivations = new double[size];
		}
		put(modInputs);
		put(modBias);
		put(modActivations);
		modBiasModified = false;
	}
	
	@Override
	public void reset() {
		Arrays.fill(modInputs, 0);
		Arrays.fill(modActivations, 0);
		super.reset();
	}
	
	/**
	 * Set the modulatory bias for the given neuron.
	 */
	public void setBias(int neuronIndex, double b) {
		modBias[neuronIndex] = b;
		modBiasModified = true;
	}

	@Override
	public void step() {
		if (modBiasModified) {
			put(modBias);
		}
		put(modInputs); // neuron modulatory inputs are calculated by synapse model in previous simulation step.
		super.step();
	}

	@Override
	public void run() {
		int neuronID = getGlobalId();
		if (neuronID >= size)
			return;
		modInputs[neuronID] += modBias[neuronID];
		modActivations[neuronID] = (Math.tanh(modInputs[neuronID] * 0.5) + 1) * 0.5;
		modInputs[neuronID] = 0;
		super.run();
	}

	@Override
	public ComponentCollection createCollection(int size) {
		return new RisiModulatoryNeuronCollection(size);
	}
	
	@Override
	public void ensureInputsAreFresh() {
		if (inputsStale) {
			get(modInputs);
		}
		super.ensureInputsAreFresh();
	}
	
	@Override
	public void ensureOutputsAreFresh() {
		if (outputsStale) {
			get(modActivations);
		}
		super.ensureOutputsAreFresh();
	}

	public double[] getModInputs() {
		return modInputs;
	}
	
	public double[] getModActivations() {
		return modActivations;
	}
}