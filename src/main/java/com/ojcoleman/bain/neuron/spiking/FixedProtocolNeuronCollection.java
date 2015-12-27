package com.ojcoleman.bain.neuron.spiking;

import com.ojcoleman.bain.base.NeuronCollection;

/**
 * A that produces pre-determined spike patterns.
 * 
 * @author Oliver J. Coleman
 */
public class FixedProtocolNeuronCollection extends NeuronCollection<FixedProtocolNeuronConfiguration> {
	long[] simStep = new long[1]; // Current simulation step.

	// Config parameters.
	int[] configSpikePatternPeriod; // Duration of protocol, in number of simulation steps.
	int[] configProtocolIndex; // Index into configSpikeProtocol for start of each protocol.
	boolean[] configSpikeProtocol; // Look-up table for protocol, one element for each simulation step (for each protocol, packed array).
	double[] configSpikePotential;
	double[] configRestPotential;

	public FixedProtocolNeuronCollection(int size) {
		this.size = size;
		init();
	}

	public void init() {
		super.init();
		if (network != null) {
			configSpikePatternPeriod = new int[configs.size()];
			configProtocolIndex = new int[configs.size()];
			configSpikePotential = new double[configs.size()];
			configRestPotential = new double[configs.size()];

			int totalDurations = 0;
			for (int c = 0; c < configs.size(); c++) {
				FixedProtocolNeuronConfiguration config = configs.get(c);
				// Use ceiling to avoid truncating spikes right at the end.
				configSpikePatternPeriod[c] = (int) Math.ceil(config.spikePatternPeriod * network.getTimeResolution());
				configProtocolIndex[c] = totalDurations;
				totalDurations += configSpikePatternPeriod[c];
				configSpikePotential[c] = config.spikePotential;
				configRestPotential[c] = config.restPotential;
			}

			configSpikeProtocol = new boolean[totalDurations];
			for (int c = 0; c < configs.size(); c++) {
				FixedProtocolNeuronConfiguration config = configs.get(c);
				int spikeDuration = (int) Math.round(config.spikeDuration * network.getTimeResolution());
				for (int s = 0; s < config.spikeTimings.length; s++) {
					int spikeStart = (int) Math.round(config.spikeTimings[s] * network.getTimeResolution());
					for (int d = spikeStart; d < configSpikePatternPeriod[c] && d <= spikeStart + spikeDuration; d++) {
						configSpikeProtocol[configProtocolIndex[c] + d] = true;
					}
				}
			}
		}

		// Transfer config data to Aparapi kernel.
		put(configSpikePatternPeriod);
		put(configProtocolIndex);
		put(configSpikeProtocol);
		put(configSpikePotential);
		put(configRestPotential);
	}

	@Override
	public void step() {
		simStep[0] = network.getStep();
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
		outputs[neuronID] = configSpikeProtocol[configProtocolIndex[configID] + stepInProtocol] ? configSpikePotential[configID] : configRestPotential[configID];
		super.run();
	}

	@Override
	public FixedProtocolNeuronConfiguration getConfigSingleton() {
		return new FixedProtocolNeuronConfiguration();
	}

	@Override
	public void ensureStateVariablesAreFresh() {
		// No state variables.
	}

	@Override
	public FixedProtocolNeuronCollection createCollection(int size) {
		return new FixedProtocolNeuronCollection(size);
	}
}