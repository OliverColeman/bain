package com.ojcoleman.bain.neuron.rate;

import com.ojcoleman.bain.base.ComponentConfiguration;
import com.ojcoleman.bain.base.NeuronConfiguration;

/**
 * Configuration for {@link SoltoggioModulatoryNeuronCollection}.
 * 
 * @author Oliver J. Coleman
 */
public class SoltoggioModulatoryNeuronConfiguration extends NeuronConfiguration {
	/**
	 * Indicates that the neuron is a modulatory neuron instead of a regular neuron.
	 */
	public boolean modulatory;

	static String[] parameterLabels = { "modulatory" };

	/**
	 * Create a SoltoggioModulatoryNeuronConfiguration. This is used for retrieving a configuration singleton or a default configuration.
	 */
	public SoltoggioModulatoryNeuronConfiguration() {
		modulatory = false;
	}
    
	@Override
	public String[] getParameterNames() {
		return parameterLabels;
	}

	@Override
	public String[] getPresetNames() {
		return new String[] { "default" };
	}

	@Override
	public ComponentConfiguration getPreset(int index) {
		if (index == 0) {
			return new SoltoggioModulatoryNeuronConfiguration();
		}
		return null;
	}

	@Override
	public ComponentConfiguration createConfiguration() {
		return new SoltoggioModulatoryNeuronConfiguration();
	}

}