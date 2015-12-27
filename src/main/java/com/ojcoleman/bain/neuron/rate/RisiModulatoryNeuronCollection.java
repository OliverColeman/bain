package com.ojcoleman.bain.neuron.rate;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;

import com.ojcoleman.bain.base.*;

/**
 * Implements the neuron model described by S. Risi, K.O. Stanley (2012) A Uniﬁed Approach to Evolving Plasticity and
 * Neural Geometry.
 * 
 * This model must be used in combination with {@link com.ojcoleman.bain.synapse.rate.RisiModulatorySynapseCollection}
 * 
 * @author Oliver J. Coleman
 */
public class RisiModulatoryNeuronCollection<C extends RisiModulatoryNeuronConfiguration> extends SigmoidBipolarNeuronCollection<C> {
	private static final NumberFormat nf = new DecimalFormat("0.00");

	/**
	 * The bias value for synaptic plasticity modulation for each neuron configuration.
	 */
	protected double[] modBias;

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
	 * Create an empty RisiModulatorySigmoidNeuronCollection. This is used for retrieving a singleton to create a
	 * non-empty collection with.
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
			modActivations = new double[size];
		}
		if (modBias == null || modBias.length != configs.size()) {
			modBias = new double[configs.size()];
		}
		for (int ci = 0; ci < configs.size(); ci++) {
			modBias[ci] = configs.get(ci).modBias;
		}

		put(modInputs);
		put(modActivations);
		put(modBias);
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
		/*
		 * if (network.debug()) { System.out.println("\n\n" + Arrays.toString(configs.get(0).getParameterNames())); for
		 * (ComponentConfiguration c : configs) { System.out.println(Arrays.toString(c.getParameterValues())); }
		 * System.out.println("n\tc\tmi\tma\ti\to"); }
		 */
		super.step();
	}

	@Override
	public void run() {
		int neuronID = getGlobalId();
		if (neuronID >= size)
			return;
		int configID = componentConfigIndexes[neuronID];
		//String out = neuronID + "\t" + configID + "\t";
		modInputs[neuronID] += modBias[configID];
		//out += nf.format(modInputs[neuronID]) + "\t";
		modActivations[neuronID] = (Math.tanh(modInputs[neuronID] * 0.5) + 1) * 0.5;
		//out += nf.format(modActivations[neuronID]) + "\t";
		modInputs[neuronID] = 0;
		super.run();
		//out += nf.format(inputs[neuronID]) + "\t" + nf.format(outputs[neuronID]);
		//if (network.debug()) {
		//	System.out.println(out);
		//}
	}

	@Override
	public ComponentCollection createCollection(int size) {
		return new RisiModulatoryNeuronCollection<RisiModulatoryNeuronConfiguration>(size);
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
		super.ensureStateVariablesAreFresh();
		get(modInputs);
		get(modActivations);
	}

	public double[] getModInputs() {
		return modInputs;
	}

	public double[] getModActivations() {
		return modActivations;
	}

	@Override
	public ComponentConfiguration getConfigSingleton() {
		return new RisiModulatoryNeuronConfiguration();
	}
}