package ojc.bain.neuron.rate;

import ojc.bain.base.*;
import ojc.bain.neuron.spiking.FixedFrequencyNeuronConfiguration;

/**
 * Implements rate-based neurons that use a bipolar Sigmoidal activation function.
 * 
 * @see SigmoidNeuronConfiguration
 * 
 * @author Oliver J. Coleman
 */
public class SigmoidBipolarNeuronCollection extends NeuronCollectionWithBias<SigmoidNeuronConfiguration> {
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
		configSlope = new double[configs.size()];
		if (network != null) {
			for (int i = 0; i < configs.size(); i++) {
				SigmoidNeuronConfiguration config = configs.get(i);
				configSlope[i] = config.slope;
			}
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