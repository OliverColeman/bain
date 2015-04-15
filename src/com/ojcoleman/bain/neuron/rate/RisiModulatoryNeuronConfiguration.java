package com.ojcoleman.bain.neuron.rate;

import com.ojcoleman.bain.base.ComponentConfiguration;
import com.ojcoleman.bain.base.NeuronConfiguration;

/**
 * Configuration for {@link SigmoidNeuronCollection}.
 * 
 * @author Oliver J. Coleman
 */
public class RisiModulatoryNeuronConfiguration extends SigmoidNeuronConfiguration {
	/**
	 * The bias value for synaptic plasticity modulation. Default is 0.
	 */
	public double modBias;

	/**
	 * Create a RisiModulatoryNeuronConfiguration. This is used for retrieving a configuration singleton or a default configuration.
	 */
	public RisiModulatoryNeuronConfiguration() {
	}

	/**
	 * Create a RisiModulatoryNeuronConfiguration.
	 * 
	 * @param slope The slope of the Sigmoid function.
	 */
	public RisiModulatoryNeuronConfiguration(double slope, double modBias) {
		this.slope = slope;
		this.modBias = modBias;
	}

	@Override
	public String[] getParameterNames() {
		return new String[] { "slope", "modBias" };
	}

	@Override
	public String[] getPresetNames() {
		return new String[] { "default" };
	}

	@Override
	public ComponentConfiguration getPreset(int index) {
		if (index == 0) {
			return new RisiModulatoryNeuronConfiguration();
		}
		return null;
	}

	@Override
	public ComponentConfiguration createConfiguration() {
		return new RisiModulatoryNeuronConfiguration();
	}

}