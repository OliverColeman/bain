package com.ojcoleman.bain.neuron.spiking;

import com.ojcoleman.bain.base.ComponentConfiguration;
import com.ojcoleman.bain.base.NeuronConfiguration;

/**
 * Configuration for {@link FixedFrequencyNeuronCollection}.
 * 
 * @author Oliver J. Coleman
 */
public class FixedFrequencyNeuronConfiguration extends NeuronConfiguration {
	public double spikingPeriod; // Period between spikes.

	/**
	 * Create a NeuronFixedFrequencyConfig. This is only used for retrieving a configuration singleton.
	 */
	public FixedFrequencyNeuronConfiguration() {
		this.spikingPeriod = 0.1;
	}

	/**
	 * Create a NeuronFixedFrequencyConfig.
	 * 
	 * @param spikingPeriod The period, in seconds, between spikes.
	 */
	public FixedFrequencyNeuronConfiguration(double spikingPeriod) {
		this.spikingPeriod = spikingPeriod;
	}

	/**
	 * Sets a new spiking period.
	 */
	public void spikingPeriod(double spikingPeriod) {
		this.spikingPeriod = spikingPeriod;
	}

	@Override
	public String[] getParameterNames() {
		return new String[] { "spikingPeriod" };
	}

	@Override
	public String[] getPresetNames() {
		return new String[] {};
	}

	@Override
	public ComponentConfiguration getPreset(int index) {
		return null;
	}

	@Override
	public ComponentConfiguration createConfiguration() {
		return new FixedFrequencyNeuronConfiguration();
	}

}