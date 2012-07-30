package neuron;

/**
 * A neuron that produces pre-determined spike patterns.
 * @author Oliver J. Coleman
 */
public class NeuronFixed extends Neuron {
	// Presentation period of spike pattern.
	int spikePatternPeriod;

	// Timings of spikes.
	int[] spikeTimings;
	
	int spikeTimingIndex = 0, time = 0;
			
	
	/**
	 * Create a NeuronTest with a custom firing pattern.
	 * @param spikePatternPeriod The total length of the pattern (ms).
	 * @param spikeTimings An array containing the times (ms) of each spike, relative to the beginning of the pattern.
	 */
	public NeuronFixed(int spikePatternPeriod, int[] spikeTimings) {
		this.spikePatternPeriod = spikePatternPeriod;
		this.spikeTimings = spikeTimings;
	}
	
	/**
	 * Create a NeuronTest with a regular firing pattern.
	 * @param spikePatternPeriod The total length of the pattern (ms).
	 * @param spikingPeriod The time between spikes (ms).
	 * @param startDelay Time before spiking begins (ms).
	 */
	public NeuronFixed(int spikePatternPeriod, int spikingPeriod, int startDelay) {
		this.spikePatternPeriod = spikePatternPeriod;

		int spikeCount = spikePatternPeriod / spikingPeriod;
		spikeTimings = new int[spikeCount];
				
		int st = startDelay;
		for (int s = 0; s < spikeCount; s++) {
			spikeTimings[s] = st;
			st += spikingPeriod;
		}
	}
	
	/**
	 * Sets a new firing pattern. The Neuron is reset and will begin at the start of the new pattern.
	 * @param spikeTimings An array containing the times (ms) of each spike, relative to the beginning of the pattern.
	 */
	public void setSpikeTimings(int[] spikeTimings) {
		this.spikeTimings = spikeTimings;
		this.reset();
	}

	/**
	 * @see neuron.Neuron#reset()
	 */
	public void reset(){
		output = 0;
		spikeTimingIndex = 0;
		time = 0;
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
		if (spikeTimingIndex < spikeTimings.length && spikeTimings[spikeTimingIndex] == time) {
			output = 1;
			spikeTimingIndex++;
		}
		else {
			output = 0;
		}
		
		if (time < spikePatternPeriod-1) {
			time++;
		}
		else {
			time = 0;
			spikeTimingIndex = 0;
		}

		return output;
	}
}