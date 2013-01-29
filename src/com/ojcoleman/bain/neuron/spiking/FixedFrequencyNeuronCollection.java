package com.ojcoleman.bain.neuron.spiking;


import com.amd.aparapi.Kernel;
import com.ojcoleman.bain.base.*;

/**
 * A collection of neurons that produce spikes at a fixed frequency. The frequency of spiking for each is determined by its associated
 * {@link FixedFrequencyNeuronConfiguration}.
 * 
 * @author Oliver J. Coleman
 */
public class FixedFrequencyNeuronCollection extends NeuronCollection<FixedFrequencyNeuronConfiguration> {
	int[] configSpikingPeriod;
	double[] configSpikePotential;
	double[] configRestPotential;

	// Used in the Aparapi kernel to pass the simulation step.
	long[] simStep = new long[1];

	/**
	 * Create a NeuronFixedFrequencyCollection.
	 * 
	 * @param size The size of this collection.
	 */
	public FixedFrequencyNeuronCollection(int size) {
		this.size = size;
		init();
	}

	public void init() {
		super.init();
		configSpikingPeriod = new int[configs.size()];
		configSpikePotential = new double[configs.size()];
		configRestPotential = new double[configs.size()];

		if (network != null) {
			for (int i = 0; i < configs.size(); i++) {
				FixedFrequencyNeuronConfiguration config = configs.get(i);
				configSpikingPeriod[i] = (int) Math.round(config.spikingPeriod * network.getTimeResolution());
				configSpikePotential[i] = config.spikePotential;
				configRestPotential[i] = config.restPotential;
			}
		}

		// Transfer data to Aparapi kernel.
		put(configSpikingPeriod);
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
		int spikePeriod = configSpikingPeriod[configID];
		outputs[neuronID] = (simStep[0] % spikePeriod == 0) ? configSpikePotential[configID] : configRestPotential[configID];
		super.run();
	}

	@Override
	public ComponentConfiguration getConfigSingleton() {
		return new FixedFrequencyNeuronConfiguration();
	}

	@Override
	public void ensureStateVariablesAreFresh() {
	}

	@Override
	public ComponentCollection createCollection(int size) {
		return new FixedFrequencyNeuronCollection(size);
	}
}