package ojc.bain.neuron;

import ojc.bain.base.*;

import com.amd.aparapi.Kernel;

/**
 * A collection of neurons that produce spikes at a fixed frequency. The frequency of spiking for each ojc.bain.neuron is
 * determined by its associated {@link FixedFrequencyNeuronConfiguration}.
 * 
 * @author Oliver J. Coleman
 */
public class FixedFrequencyNeuronCollection extends NeuronCollection<FixedFrequencyNeuronConfiguration> {
	int[] configSpikingPeriod;

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
		if (simulation != null) {
			for (int i = 0; i < configs.size(); i++) {
				configSpikingPeriod[i] = (int) Math.round(configs.get(i).spikingPeriod * simulation.getTimeResolution());
			}
		}

		// Transfer data to Aparapi kernel.
		put(componentConfigIndexes);
		put(configSpikingPeriod);
		put(neuronOutputs);
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
		int spikePeriod = configSpikingPeriod[configID];
		neuronOutputs[neuronID] = (simStep[0] % spikePeriod == 0) ? 1 : 0;
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