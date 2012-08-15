package ojc.bain.neuron;

import ojc.bain.base.ComponentConfiguration;
import ojc.bain.base.NeuronCollection;

/**
 * A that produces randomised spike trains.
 * 
 * TODO: Refactor to work in NeuronCollection framework. TODO: Allow different probability distributions, use more efficient
 * RNG(s).
 * 
 * @author Oliver J. Coleman
 */
public class NeuronRandom extends NeuronCollection {
	NeuronRandomConfig config;

	/**
	 * Create a NeuronRandom with the specified configuration.
	 */
	public NeuronRandom(NeuronRandomConfig config) {
		this.config = config;
	}

	/**
	 * @see ojc.bain.base.NeuronCollection#step()
	 */
	public double step() {
		neuronOutputs = (Math.random() > config.threshold) ? 1 : 0;
		return neuronOutputs;
	}

	@Override
	public void setConfig(ComponentConfiguration config) {
		this.config = (NeuronRandomConfig) config;
	}

	@Override
	public ComponentConfiguration getConfig() {
		return config;
	}

	@Override
	public ComponentConfiguration getConfigSingleton() {
		return new NeuronRandomConfig();
	}
}