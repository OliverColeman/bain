package com.ojcoleman.bain.synapse.rate;

import java.util.Arrays;

import com.ojcoleman.bain.base.ComponentCollection;
import com.ojcoleman.bain.base.ComponentConfiguration;
import com.ojcoleman.bain.base.SynapseCollection;
import com.ojcoleman.bain.synapse.spiking.Pfister2006SynapseConfiguration;


/**
 * Implements synapses with a fixed efficacy (weight).
 * 
 * @author Oliver J. Coleman
 */
public class FixedSynapseCollection extends SynapseCollection {
	public FixedSynapseCollection(int size) {
		this.size = size;
		init();
	}

	@Override
	public void init() {
		super.init();
		stateVariablesStale = false;
	}

	@Override
	public void run() {
		int synapseID = this.getGlobalId();
		if (synapseID >= size)
			return;
		super.run();
	}

	@Override
	public ComponentConfiguration getConfigSingleton() {
		return null;
	}

	@Override
	public ComponentCollection createCollection(int size) {
		return new FixedSynapseCollection(size);
	}
}
