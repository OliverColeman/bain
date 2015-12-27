package com.ojcoleman.bain.neuron.spiking;

import com.ojcoleman.bain.base.NeuronConfiguration;

/**
 * A neuron model that produces pre-determined spike patterns. The spikes are modelled as instantaneous transitions between the resting potential and the spike
 * potential.
 * 
 * @author Oliver J. Coleman
 */
public class FixedProtocolNeuronConfiguration extends NeuronConfiguration {
	/**
	 * Presentation period of spike pattern, in seconds.
	 */
	public double spikePatternPeriod;

	/**
	 * Times of the start of spikes relative to the start of the protocol, in seconds. Spike start times should be less than ({@link #spikePatternPeriod} +
	 * {@link #spikeDuration}), otherwise they will be truncated.
	 */
	public double[] spikeTimings;

	/**
	 * The duration of a spike in seconds. Default duration is 1ms.
	 */
	public double spikeDuration = 0.001;

	/**
	 * Create a FixedProtocolNeuronConfiguration. This is only used for retrieving a configuration singleton.
	 */
	public FixedProtocolNeuronConfiguration() {
	}

	/**
	 * Create a FixedProtocolNeuronConfiguration with a custom firing pattern.
	 * 
	 * @param spikePatternPeriod The total length of the pattern (s).
	 * @param spikeTimings An array containing the times (s) of each spike, relative to the beginning of the pattern.
	 */
	public FixedProtocolNeuronConfiguration(double spikePatternPeriod, double[] spikeTimings) {
		this.spikePatternPeriod = spikePatternPeriod;
		this.spikeTimings = spikeTimings;
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
	public FixedProtocolNeuronConfiguration getPreset(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FixedProtocolNeuronConfiguration createConfiguration() {
		return new FixedProtocolNeuronConfiguration();
	}
}