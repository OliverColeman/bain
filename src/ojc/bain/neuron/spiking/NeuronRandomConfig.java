package ojc.bain.neuron.spiking;

import ojc.bain.base.ComponentConfiguration;

/**
 * Configuration for NeuronRandom.
 * 
 * TODO: Refactor to work in NeuronCollection framework. TODO: Allow different probability distributions, use more efficient RNG(s).
 * 
 * @author Oliver J. Coleman
 */
public class NeuronRandomConfig extends ComponentConfiguration {
	double rate, threshold;

	/**
	 * Create a NeuronRandomConfig. This is generally only used for retrieving a configuration singleton.
	 */
	public NeuronRandomConfig() {
		rate = 1;
		init();
	}

	/**
	 * Create a NeuronRandom with the given firing rate. The rate describes how likely the Neuron is to spike every second: a value of 0 would cause the Neuron
	 * to never spike, a value of 1 would cause the Neuron to spike once per second on average.
	 * 
	 * @param rate The firing rate.
	 */
	public NeuronRandomConfig(double rate) {
		this.rate = rate;
		init();
	}

	public void init() {
		threshold = rate / timeResolution;
	}

	@Override
	public String[] getParameterNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getPresetNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ComponentConfiguration getPreset(int index) {
		// TODO Auto-generated method stub
		return null;
	}
}