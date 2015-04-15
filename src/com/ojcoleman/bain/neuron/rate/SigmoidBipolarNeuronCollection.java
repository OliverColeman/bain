package com.ojcoleman.bain.neuron.rate;

import com.ojcoleman.bain.base.*;
import com.ojcoleman.bain.neuron.spiking.FixedFrequencyNeuronConfiguration;

/**
 * Implements rate-based neurons that use a bipolar Sigmoidal activation function.
 * 
 * @see SigmoidNeuronConfiguration
 * 
 * @author Oliver J. Coleman
 */
public class SigmoidBipolarNeuronCollection<C extends SigmoidNeuronConfiguration> extends NeuronCollectionWithBias<C> {
	double[] configSlope;

	/**
	 * Create an empty SigmoidNeuronCollection. This is used for retrieving a singleton to create a non-empty collection with.
	 */
	public SigmoidBipolarNeuronCollection() {
		init();
	}

	/**
	 * Create a SigmoidNeuronCollection.
	 * 
	 * @param size The size of this collection.
	 */
	public SigmoidBipolarNeuronCollection(int size) {
		this.size = size;
		init();
	}

	public void init() {
		super.init();
		if (configSlope == null || configSlope.length != configs.size()){
			configSlope = new double[configs.size()];
		}
		for (int i = 0; i < configs.size(); i++) {
			SigmoidNeuronConfiguration config = configs.get(i);
			configSlope[i] = config.slope;
		}
	
		// Transfer data to Aparapi kernel.
		put(configSlope);
	}

	@Override
	public void run() {
		int neuronID = getGlobalId();
		if (neuronID >= size)
			return;
		int configID = componentConfigIndexes[neuronID];
		inputs[neuronID] += bias[neuronID];
		outputs[neuronID] = 2.0 / (1.0 + Math.exp(-(inputs[neuronID] * configSlope[configID]))) - 1.0;
		super.run();
	}
	
	@Override
	public double getMinimumPossibleOutputValue() {
		return -1;
	}

	@Override
	public double getMaximumPossibleOutputValue() {
		return 1;
	}

	@Override
	public ComponentConfiguration getConfigSingleton() {
		return new SigmoidNeuronConfiguration();
	}

	@Override
	public void ensureStateVariablesAreFresh() {
	}

	@Override
	public ComponentCollection createCollection(int size) {
		return new SigmoidBipolarNeuronCollection(size);
	}
}