package neuron;

import base.ComponentConfiguration;

/**
 * A neuron that produces pre-determined spike patterns.
 * @author Oliver J. Coleman
 */
public class NeuronFixed extends Neuron {
	NeuronFixedConfig config;
	
	int spikeTimingIndex = 0; 
	int stepIndex = 0;
	
	/**
	 * Create a NeuronTest with the specified configuration.
	 * @param config The configuration to use for this NeuronFixed.
	 */
	public NeuronFixed(NeuronFixedConfig config) {
		this.config = config;
	}
	
	/**
	 * @see neuron.Neuron#reset()
	 */
	public void reset(){
		output = 0;
		spikeTimingIndex = 0;
		stepIndex = 0;
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
		double time = stepIndex * config.getStepPeriod(); 
		if (spikeTimingIndex < config.spikeTimings.length && config.spikeTimings[spikeTimingIndex] <= time+config.getStepPeriod()/10) {
			output = 1;
			spikeTimingIndex++;
		}
		else {
			output = 0;
		}
		
		if (time < config.spikePatternPeriod - config.getStepPeriod()) {
			stepIndex++;
		}
		else {
			stepIndex = 0;
			spikeTimingIndex = 0;
		}

		return output;
	}

	@Override
	public void setConfig(ComponentConfiguration config) {
		this.config = (NeuronFixedConfig) config;
		reset();
	}

	@Override
	public ComponentConfiguration getConfig() {
		return config;
	}

	@Override
	public ComponentConfiguration getConfigSingleton() {
		return new NeuronFixedConfig();
	}
}