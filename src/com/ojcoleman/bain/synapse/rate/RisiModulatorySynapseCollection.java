package com.ojcoleman.bain.synapse.rate;

import java.util.Arrays;

import com.ojcoleman.bain.base.ComponentCollection;
import com.ojcoleman.bain.base.ComponentConfiguration;
import com.ojcoleman.bain.base.SynapseCollection;
import com.ojcoleman.bain.neuron.rate.RisiModulatoryNeuronCollection;
import com.ojcoleman.bain.synapse.spiking.Clopath2010SynapseConfiguration;


/**
 * Implements the plastic synapse model described by S. Risi, K.O. Stanley (2012) A Unified Approach to Evolving
 * Plasticity and Neural Geometry. This model must be used in combination with
 * {@link com.ojcoleman.bain.neuron.rate.RisiModulatoryNeuronCollection}
 * 
 * @author Oliver J. Coleman
 */
public class RisiModulatorySynapseCollection extends Niv2002SynapseCollection<RisiModulatorySynapseConfiguration> {
	private static final RisiModulatorySynapseConfiguration configSingleton = new RisiModulatorySynapseConfiguration();

	// Parameters from configs.
	protected boolean[] modulatory;

	/**
	 * The {@link com.ojcoleman.bain.neuron.rate.RisiModulatoryNeuronCollection#modInputs} from the associated
	 * RisiModulatoryNeuronCollection.
	 */
	protected double[] neuronModInputs;

	/**
	 * The {@link com.ojcoleman.bain.neuron.rate.RisiModulatoryNeuronCollection#modActivations} from the associated
	 * RisiModulatoryNeuronCollection.
	 */
	protected double[] neuronModActivations;

	public RisiModulatorySynapseCollection(int size) {
		super(size);
	}

	@Override
	public void init() {
		super.init();
		if (modulatory == null || modulatory.length != configs.size()) {
			modulatory = new boolean[configs.size()];
		}
		for (int ci = 0; ci < configs.size(); ci++) {
			modulatory[ci] = configs.get(ci).modulatory;
		}
	
		if (network != null) {
			neuronModInputs = ((RisiModulatoryNeuronCollection) network.getNeurons()).getModInputs();
			neuronModActivations = ((RisiModulatoryNeuronCollection) network.getNeurons()).getModActivations();
		}

		// In case explicit mode is being used for the Aparapi kernel.
		put(modulatory);
	}

	@Override
	public void step() {
		// At the moment Aparapi doesn't allow sharing buffers between kernels
		// or allow kernels with multiple entry points in a way that is
		// compatible with a framework such as this. Thus we must ensure that
		// fresh versions of the following buffers are available to this kernel
		// by "putting" them there.
		network.getNeurons().ensureInputsAreFresh(); // Get fresh versions from the neuron kernel.
		network.getNeurons().ensureOutputsAreFresh();
		put(neuronModInputs); // neuron modulatory inputs are calculated in run(), and are reset to 0 by the
								// neuron model once it's made use of them.
		put(neuronModActivations); // neuron modulatory activation levels are used in run() to update synapse weights
									// via the plasticity rule.
		super.step();
		get(neuronModInputs); // See note above.
	}

	@Override
	public void run() {
		int synapseID = this.getGlobalId();
		if (synapseID >= size)
			return;

		int configID = componentConfigIndexes[synapseID];
		outputs[synapseID] = neuronOutputs[preIndexes[synapseID]] * efficacy[synapseID];
		if (modulatory[synapseID]) {
			neuronModInputs[postIndexes[synapseID]] += outputs[synapseID];
			// Modulatory synapses are not plastic.
		} else {
			neuronInputs[postIndexes[synapseID]] += outputs[synapseID];

			// Update synapse weight via plasticity rule.
			double delta = neuronModActivations[postIndexes[synapseID]] * n[configID] * (
					a[configID] * neuronOutputs[preIndexes[synapseID]] * neuronOutputs[postIndexes[synapseID]] + 
					b[configID] * neuronOutputs[preIndexes[synapseID]] + 
					c[configID] * neuronOutputs[postIndexes[synapseID]] + 
					d[configID]);
			efficacy[synapseID] += delta;
			if (efficacy[synapseID] < minEfficacy[configID]) efficacy[synapseID] = minEfficacy[configID];
			else if (efficacy[synapseID] > maxEfficacy[configID]) efficacy[synapseID] = maxEfficacy[configID];
		}
		// We don't call super.run() as we've performed everything it does and don't want it to change what we've
		// done.
		
	}

	@Override
	public ComponentConfiguration getConfigSingleton() {
		return configSingleton;
	}

	@Override
	public ComponentCollection createCollection(int size) {
		return new RisiModulatorySynapseCollection(size);
	}
}
