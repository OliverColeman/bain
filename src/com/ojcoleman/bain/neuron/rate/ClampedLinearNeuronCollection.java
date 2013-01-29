package com.ojcoleman.bain.neuron.rate;

import com.ojcoleman.bain.base.*;

/**
 * Implements neurons that pass through received input, but chop-off values below 0 or above 1.
 * 
 * @author Oliver J. Coleman
 */
public class ClampedLinearNeuronCollection extends NeuronCollectionWithBias {
	/**
	 * Create a ClampedLinearNeuronCollection.java.
	 * 
	 * @param size The size of this collection.
	 */
	public ClampedLinearNeuronCollection(int size) {
		this.size = size;
		init();
	}

	@Override
	public void run() {
		int neuronID = getGlobalId();
		if (neuronID >= size)
			return;
		outputs[neuronID] = Math.max(Math.min(inputs[neuronID] + bias[neuronID], 1), 0);
		super.run();
	}

	@Override
	public ComponentConfiguration getConfigSingleton() {
		return null;
	}

	@Override
	public void ensureStateVariablesAreFresh() {
	}

	@Override
	public ComponentCollection createCollection(int size) {
		return new ClampedLinearNeuronCollection(size);
	}
}