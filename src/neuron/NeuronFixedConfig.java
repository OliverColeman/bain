package neuron;

import base.ComponentConfiguration;

/**
 * A neuron that produces pre-determined spike patterns.
 * @author Oliver J. Coleman
 */
public class NeuronFixedConfig extends ComponentConfiguration {
	// Presentation period of spike pattern.
	double spikePatternPeriod;
	// Timings of spikes.
	double[] spikeTimings;
	
	/**
	 * Create a NeuronFixedConfig. This is only used for retrieving a configuration singleton.
	 */
	public NeuronFixedConfig() {
	}
	
	/**
	 * Create a NeuronFixedConfig with a custom firing pattern.
	 * @param spikePatternPeriod The total length of the pattern (s).
	 * @param spikeTimings An array containing the times (s) of each spike, relative to the beginning of the pattern.
	 */
	public NeuronFixedConfig(double spikePatternPeriod, double[] spikeTimings) {
		this.spikePatternPeriod = spikePatternPeriod;
		this.spikeTimings = spikeTimings;
	}
	
	/**
	 * Create a NeuronFixedConfig with a regular firing pattern.
	 * @param spikePatternPeriod The total length of the pattern (s).
	 * @param spikingPeriod The time between spikes (s).
	 * @param startDelay Time before spiking begins (s).
	 */
	public NeuronFixedConfig(double spikePatternPeriod, double spikingPeriod, double startDelay) {
		this.spikePatternPeriod = spikePatternPeriod;

		int spikeCount = (int) (spikePatternPeriod / spikingPeriod);
		spikeTimings = new double[spikeCount];
				
		double st = startDelay;
		for (int s = 0; s < spikeCount; s++) {
			spikeTimings[s] = st;
			st += spikingPeriod;
		}
	}
	
	/**
	 * Sets a new firing pattern. The Neuron is reset and will begin at the start of the new pattern.
	 * @param spikeTimings An array containing the times (ms) of each spike, relative to the beginning of the pattern.
	 */
	public void setSpikeTimings(double[] spikeTimings) {
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
}