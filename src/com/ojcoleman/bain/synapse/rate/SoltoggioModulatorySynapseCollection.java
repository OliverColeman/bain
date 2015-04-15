package com.ojcoleman.bain.synapse.rate;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;

import com.ojcoleman.bain.base.ComponentCollection;
import com.ojcoleman.bain.base.ComponentConfiguration;
import com.ojcoleman.bain.neuron.rate.SoltoggioModulatoryNeuronCollection;
import com.ojcoleman.bain.neuron.rate.SoltoggioModulatoryNeuronConfiguration;

/**
 * Implements the plastic synapse model described by A. Soltoggio, J. Bullinaria, C. Mattiussi, P. Durr, D. Floreano (2008) Evolutionary Advantages of Neuromodulated Plasticity in Dynamic, Reward-based Scenarios. This model must be used in combination with
 * {@link com.ojcoleman.bain.neuron.rate.SoltoggioModulatoryNeuronCollection}
 * 
 * @author Oliver J. Coleman
 */
public class SoltoggioModulatorySynapseCollection extends Niv2002SynapseCollection<Niv2002SynapseConfiguration> {
	private static final NumberFormat nf = new DecimalFormat("0.00");

	/**
	 * The {@link com.ojcoleman.bain.neuron.rate.SoltoggioModulatoryNeuronCollection#modInputs} from the associated
	 * SoltoggioModulatoryNeuronCollection.
	 */
	protected double[] neuronModInputs;

	/**
	 * The {@link com.ojcoleman.bain.neuron.rate.SoltoggioModulatoryNeuronCollection#modActivations} from the associated
	 * SoltoggioModulatoryNeuronCollection.
	 */
	protected double[] neuronModActivations;
	
	/**
	 * The {@link com.ojcoleman.bain.neuron.rate.SoltoggioModulatoryNeuronCollection#modulatory} from the associated
	 * SoltoggioModulatoryNeuronCollection.
	 */
	protected boolean[] neuronModulatory;

	public SoltoggioModulatorySynapseCollection(int size) {
		super(size);
	}

	@Override
	public void init() {
		super.init();
		if (network != null) {
			neuronModInputs = ((SoltoggioModulatoryNeuronCollection<SoltoggioModulatoryNeuronConfiguration>) network.getNeurons()).getModInputs();
			neuronModActivations = ((SoltoggioModulatoryNeuronCollection<SoltoggioModulatoryNeuronConfiguration>) network.getNeurons()).getModActivations();
			neuronModulatory = ((SoltoggioModulatoryNeuronCollection<SoltoggioModulatoryNeuronConfiguration>) network.getNeurons()).getModulatory();
			put(neuronModulatory); // used in run() to determine whether the presynaptic neuron is a modulatory neuron.
		}
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
		if (synapseID >= size) {
			return;
		}

		int configID = componentConfigIndexes[synapseID];
		int preNeuronID = preIndexes[synapseID];
		int postNeuronID = postIndexes[synapseID];
		
		outputs[synapseID] = neuronOutputs[preNeuronID] * efficacy[synapseID];
		
		// If the pre-synaptic neuron is modulatory then add its output to the modulatory activation level of the post-synaptic neuron...
		if (neuronModulatory[preNeuronID]) {
			neuronModInputs[postNeuronID] += outputs[synapseID];
		} else { 
			// ...otherwise add its output to the regular activation level of the post-synaptic neuron and then apply the weight update rule.
			// (only connections coming from regular neurons are plastic).
			neuronInputs[postNeuronID] += outputs[synapseID];
		
			if (n[configID] != 0) {
				// Update synapse weight via plasticity rule.
				double delta = neuronModActivations[postNeuronID] * n[configID] * (
						a[configID] * neuronOutputs[preNeuronID] * neuronOutputs[postNeuronID] + 
						b[configID] * neuronOutputs[preNeuronID] + 
						c[configID] * neuronOutputs[postNeuronID] + 
						d[configID]);
				efficacy[synapseID] += delta;
				if (efficacy[synapseID] < minEfficacy[configID]) efficacy[synapseID] = minEfficacy[configID];
				else if (efficacy[synapseID] > maxEfficacy[configID]) efficacy[synapseID] = maxEfficacy[configID];
			}
		}
	}
	
	@Override
	public boolean isNotUsed(int synapseIndex) {
		assert n[componentConfigIndexes[synapseIndex]] == ((Niv2002SynapseConfiguration) getComponentConfiguration(synapseIndex)).n;
		return initialEfficacy[synapseIndex] == 0 && n[componentConfigIndexes[synapseIndex]] == 0;
	}

	@Override
	public ComponentCollection createCollection(int size) {
		return new SoltoggioModulatorySynapseCollection(size);
	}
}
