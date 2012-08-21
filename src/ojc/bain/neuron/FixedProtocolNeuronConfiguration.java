package ojc.bain.neuron;

import ojc.bain.base.ComponentConfiguration;

/**
 * A that produces pre-determined spike patterns.
 * 
 * @author Oliver J. Coleman
 */
public class FixedProtocolNeuronConfiguration extends ComponentConfiguration {
	/**
	 * Presentation period of spike pattern, in seconds.
	 */
	public double spikePatternPeriod;

	/**
	 * Timings of spikes relative to start of protocol, in seconds.
	 */
	public double[] spikeTimings;

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
	public ComponentConfiguration getPreset(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ComponentConfiguration createConfiguration() {
		return new FixedProtocolNeuronConfiguration();
	}
}