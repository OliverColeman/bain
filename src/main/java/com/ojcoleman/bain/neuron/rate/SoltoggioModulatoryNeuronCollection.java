package com.ojcoleman.bain.neuron.rate;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;

import com.ojcoleman.bain.base.*;

/**
 * Implements the neuron model described by A. Soltoggio, J. Bullinaria, C. Mattiussi, P. Durr, D. Floreano (2008) Evolutionary Advantages of Neuromodulated Plasticity in Dynamic, Reward-based Scenarios
 * 
 * This model must be used in combination with {@link com.ojcoleman.bain.synapse.rate.SoltoggioModulatorySynapseCollection}
 * 
 * @author Oliver J. Coleman
 */
public class SoltoggioModulatoryNeuronCollection<C extends SoltoggioModulatoryNeuronConfiguration> extends NeuronCollectionWithBias<C> {
	private static final NumberFormat nf = new DecimalFormat("0.00");

	/**
	 * Indicates that the neuron is a modulatory neuron instead of a regular neuron for each neuron.
	 */
	protected boolean[] modulatory;

	/**
	 * The current synaptic plasticity modulation inputs for the neurons.
	 */
	protected double[] modInputs;

	/**
	 * The current synaptic plasticity modulation activation level for the neurons. This is used as a factor in
	 * determining the current plasticity of the incoming synapses.
	 */
	protected double[] modActivations;

	/**
	 * Create an empty SoltoggioModulatorySigmoidNeuronCollection. This is used for retrieving a singleton to create a
	 * non-empty collection with.
	 */
	public SoltoggioModulatoryNeuronCollection() {
		init();
	}

	/**
	 * Create a SoltoggioModulatorySigmoidNeuronCollection.
	 * 
	 * @param size The size of this collection.
	 */
	public SoltoggioModulatoryNeuronCollection(int size) {
		this.size = size;
		init();
	}

	@Override
	public void init() {
		super.init();

		if (modInputs == null || modInputs.length != size) {
			modInputs = new double[size];
			modActivations = new double[size];
			modulatory = new boolean[size];
		}
		if (modulatory != null && configs != null && !configs.isEmpty()){ 
			for (int neuronID = 0; neuronID < size; neuronID++) {
				modulatory[neuronID] = configs.get(componentConfigIndexes[neuronID]).modulatory;
			}
		}
		put(modInputs);
		put(modActivations);
		put(modulatory);
	}

	@Override
	public void reset() {
		Arrays.fill(modInputs, 0);
		Arrays.fill(modActivations, 0);
		super.reset();
	}

	@Override
	public void step() {
		put(modInputs); // neuron modulatory inputs are calculated by synapse model in previous simulation step.
		if (outputsModified) put(modActivations);
		super.step();
	}

	@Override
	public void run() {
		int neuronID = getGlobalId();
		if (neuronID >= size)
			return;

		inputs[neuronID] += bias[neuronID];
		outputs[neuronID] = Math.tanh(inputs[neuronID] * 0.5);
		
		modActivations[neuronID] = Math.tanh(modInputs[neuronID] * 0.5);
		
		super.run();
	}

	@Override
	public ComponentCollection createCollection(int size) {
		return new SoltoggioModulatoryNeuronCollection<SoltoggioModulatoryNeuronConfiguration>(size);
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
	
	@Override
	public void ensureStateVariablesAreFresh() {
		get(modInputs);
		get(modActivations);
	}

	public double[] getModInputs() {
		return modInputs;
	}
	public double[] getModActivations() {
		return modActivations;
	}
	public boolean[] getModulatory() {
		return modulatory;
	}

	@Override
	public ComponentConfiguration getConfigSingleton() {
		return new SoltoggioModulatoryNeuronConfiguration();
	}
}