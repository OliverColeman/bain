package ojc.bain.neuron;

import java.util.Arrays;

import ojc.bain.base.ComponentCollection;
import ojc.bain.base.ComponentConfiguration;
import ojc.bain.base.NeuronCollection;

/**
 * A that produces pre-determined spike patterns.
 * 
 * @author Oliver J. Coleman
 */
public class FixedProtocolNeuronCollection extends NeuronCollection<FixedProtocolNeuronConfiguration> {
	long[] simStep = new long[1]; // Current simulation step.

	// Config parameters.
	long[] configSpikePatternPeriod; // Duration of protocol, in number of simulation steps.
	int[] configProtocolIndex; // Index into configSpikeProtocol for start of each protocol.
	byte[] configSpikeProtocol; // Look-up table for protocol, one element for each simulation step (for each protocol).

	public FixedProtocolNeuronCollection(int size) {
		this.size = size;
		init();
	}

	public void init() {
		super.init();
		if (simulation != null) {
			configSpikePatternPeriod = new long[configs.size()];
			configProtocolIndex = new int[configs.size()];

			int totalDurations = 0;
			for (int c = 0; c < configs.size(); c++) {
				FixedProtocolNeuronConfiguration config = configs.get(c);
				// Use ceiling to make sure we catch a spike right at the end.
				configSpikePatternPeriod[c] = (int) Math.ceil(config.spikePatternPeriod * simulation.getTimeResolution());
				configProtocolIndex[c] = totalDurations;
				totalDurations += configSpikePatternPeriod[c];
			}

			configSpikeProtocol = new byte[totalDurations];
			for (int c = 0; c < configs.size(); c++) {
				FixedProtocolNeuronConfiguration config = configs.get(c);
				for (int s = 0; s < config.spikeTimings.length; s++) {
					int spikeStep = (int) Math.round(config.spikeTimings[s] * simulation.getTimeResolution());
					if (spikeStep < configSpikePatternPeriod[c]) {
						configSpikeProtocol[configProtocolIndex[c] + spikeStep] = 1;
					}
				}
			}
		}

		// Transfer config data to Aparapi kernel.
		put(configSpikePatternPeriod);
		put(configProtocolIndex);
		put(configSpikeProtocol);
	}

	@Override
	public void step() {
		simStep[0] = simulation.getStep();
		put(simStep);
		super.step();
	}

	@Override
	public void run() {
		int neuronID = getGlobalId();
		if (neuronID >= size)
			return;
		int configID = componentConfigIndexes[neuronID];
		int stepInProtocol = (int) (simStep[0] % configSpikePatternPeriod[configID]);
		neuronOutputs[neuronID] = configSpikeProtocol[configProtocolIndex[configID] + stepInProtocol];
		super.run();
	}

	@Override
	public ComponentConfiguration getConfigSingleton() {
		return new FixedProtocolNeuronConfiguration();
	}

	@Override
	public void ensureStateVariablesAreFresh() {
		// No state variables.
	}

	@Override
	public ComponentCollection createCollection(int size) {
		return new FixedProtocolNeuronCollection(size);
	}
}