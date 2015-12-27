package com.ojcoleman.bain.neuron.rate;

import com.ojcoleman.bain.base.ComponentConfiguration;
import com.ojcoleman.bain.base.NeuronConfiguration;

/**
 * Configuration for {@link SigmoidNeuronCollection}.
 * 
 * @author Oliver J. Coleman
 */
public class SigmoidNeuronConfiguration extends NeuronConfiguration {
	/**
	 * Slope of Sigmoid function. Default is 1.
	 */
	public double slope = 1;

	/**
	 * Create a SigmoidNeuronConfiguration. This is used for retrieving a configuration singleton or a default configuration.
	 */
	public SigmoidNeuronConfiguration() {
	}

	/**
	 * Create a SigmoidNeuronConfiguration.
	 * 
	 * @param slope The slope of the Sigmoid function.
	 */
	public SigmoidNeuronConfiguration(double slope) {
		this.slope = slope;
	}

	@Override
	public String[] getParameterNames() {
		return new String[] { "slope" };
	}

	@Override
	public String[] getPresetNames() {
		return new String[] { "default" };
	}

	@Override
	public ComponentConfiguration getPreset(int index) {
		if (index == 0) {
			return new SigmoidNeuronConfiguration();
		}
		return null;
	}

	@Override
	public ComponentConfiguration createConfiguration() {
		return new SigmoidNeuronConfiguration();
	}

}