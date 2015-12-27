package com.ojcoleman.bain.synapse.rate;

import java.util.Arrays;

import com.ojcoleman.bain.base.ComponentCollection;
import com.ojcoleman.bain.base.ComponentConfiguration;
import com.ojcoleman.bain.base.SynapseCollection;


/**
 * Implements the plastic synapse model described by Niv et al (2002) Evolution of reinforcement learning in uncertain
 * environments: A simple explanation for complex foraging behaviors.
 * 
 * @author Oliver J. Coleman
 */
public class Niv2002SynapseCollection<C extends Niv2002SynapseConfiguration> extends SynapseCollection<C> {
	private static final Niv2002SynapseConfiguration configSingleton = new Niv2002SynapseConfiguration();

	// Parameters from configs.
	protected double[] n, a, b, c, d, minEfficacy, maxEfficacy;

	public Niv2002SynapseCollection(int size) {
		this.size = size;
		init();
	}

	@Override
	public void init() {
		super.init();
		if (n == null || n.length != configs.size()) {
			n = new double[configs.size()];
			a = new double[configs.size()];
			b = new double[configs.size()];
			c = new double[configs.size()];
			d = new double[configs.size()];
			minEfficacy = new double[configs.size()];
			maxEfficacy = new double[configs.size()];
		}
		
		for (int ci = 0; ci < configs.size(); ci++) {
			n[ci] = configs.get(ci).n;
			a[ci] = configs.get(ci).a;
			b[ci] = configs.get(ci).b;
			c[ci] = configs.get(ci).c;
			d[ci] = configs.get(ci).d;
			minEfficacy[ci] = configs.get(ci).minimumEfficacy;
			maxEfficacy[ci] = configs.get(ci).maximumEfficacy;
		}
	
		// In case explicit mode is being used for the Aparapi kernel.
		put(n);
		put(a);
		put(b);
		put(c);
		put(d);
		put(minEfficacy);
		put(maxEfficacy);
	}

	@Override
	public void run() {
		int synapseID = this.getGlobalId();
		if (synapseID >= size)
			return;

		int configID = componentConfigIndexes[synapseID];
		outputs[synapseID] = neuronOutputs[preIndexes[synapseID]] * efficacy[synapseID];
		neuronInputs[postIndexes[synapseID]] += outputs[synapseID];

		// Update synapse weight via plasticity rule.
		double delta = n[configID] * (
				a[configID] * neuronOutputs[preIndexes[synapseID]] * neuronOutputs[postIndexes[synapseID]] + 
				b[configID] * neuronOutputs[preIndexes[synapseID]] + 
				c[configID] * neuronOutputs[postIndexes[synapseID]] + 
				d[configID]);
		efficacy[synapseID] += delta;
		if (efficacy[synapseID] < minEfficacy[configID]) efficacy[synapseID] = minEfficacy[configID];
		else if (efficacy[synapseID] > maxEfficacy[configID]) efficacy[synapseID] = maxEfficacy[configID];
		// We don't call super.run() as we've performed everything it does and don't want it to overwrite what we've
		// done.
	}

	@Override
	public void ensureStateVariablesAreFresh() {
	}

	@Override
	public ComponentConfiguration getConfigSingleton() {
		return configSingleton;
	}

	@Override
	public ComponentCollection createCollection(int size) {
		return new Niv2002SynapseCollection(size);
	}
}
