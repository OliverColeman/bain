package neuron;

import misc.ComponentConfiguration;

/**
 * A neuron that produces randomised spike trains.
 * 
 * TODO: Allow different probability distributions, use more efficient RNG(s).
 * 
 * @author Oliver J. Coleman
 */
public class NeuronRandom extends Neuron {
	NeuronRandomConfig config;
	
	/**
	 * Create a NeuronRandom with the specified configuration.
	 */ 
	public NeuronRandom(NeuronRandomConfig config) {
		this.config = config;
	}
	
	/**
	 * @see neuron.Neuron#spiked()
	 */
	public boolean spiked() {
		return output >= 1; 
	}
	
	
	/**
	 * @see neuron.Neuron#step()
	 */
	public double step() {
		output = (Math.random() > config.threshold) ? 1 : 0;
		return output;
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