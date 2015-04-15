package com.ojcoleman.bain.synapse.spiking;

import java.util.Arrays;

import com.ojcoleman.bain.base.ComponentCollection;
import com.ojcoleman.bain.base.SynapseCollection;


/**
 * Implementation of the model described by Pfister, J.-P., Gerstner, W.: Triplets of Spikes in a Model of Spike Timing-Dependent Plasticity. J. Neurosci. 26,
 * 9673–9682 (2006).
 * 
 * @see Pfister2006SynapseConfiguration
 * 
 * @author Oliver J. Coleman
 */
public class Pfister2006SynapseCollection extends SynapseCollection<Pfister2006SynapseConfiguration> {
	private static final Pfister2006SynapseConfiguration configSingleton = new Pfister2006SynapseConfiguration();

	double[] r1, r2, o1, o2; // Spike traces.

	// Required values for each config, copied to arrays of primitives for use
	// in kernel.
	double[] tPDecayMult, tXDecayMult, tNDecayMult, tYDecayMult, a2N, a2P, a3N, a3P;

	public Pfister2006SynapseCollection(int size) {
		this.size = size;
		init();
	}

	@Override
	public void init() {
		super.init();
		if (r1 == null || r1.length != size) {
			r1 = new double[size];
			r2 = new double[size];
			o1 = new double[size];
			o2 = new double[size];
		}
		if (tPDecayMult == null || tPDecayMult.length != configs.size()) {
			tPDecayMult = new double[configs.size()];
			tXDecayMult = new double[configs.size()];
			tNDecayMult = new double[configs.size()];
			tYDecayMult = new double[configs.size()];

			a2N = new double[configs.size()];
			a2P = new double[configs.size()];
			a3N = new double[configs.size()];
			a3P = new double[configs.size()];
		}

		if (network != null) {
			for (int c = 0; c < configs.size(); c++) {
				Pfister2006SynapseConfiguration config = configs.get(c);
				tPDecayMult[c] = (1000 / config.tPDecay) / network.getTimeResolution();
				tXDecayMult[c] = (1000 / config.tXDecay) / network.getTimeResolution();
				tNDecayMult[c] = (1000 / config.tNDecay) / network.getTimeResolution();
				tYDecayMult[c] = (1000 / config.tYDecay) / network.getTimeResolution();
				a2N[c] = config.a2N;
				a2P[c] = config.a2P;
				a3N[c] = config.a3N;
				a3P[c] = config.a3P;
			}
		}

		// Transfer data to Aparapi kernel.
		put(r1);
		put(r2);
		put(o1);
		put(o2);
		put(tPDecayMult);
		put(tXDecayMult);
		put(tNDecayMult);
		put(tYDecayMult);
		put(a2N);
		put(a2P);
		put(a3N);
		put(a3P);
		stateVariablesStale = false;
	}

	public void reset() {
		super.reset();
		Arrays.fill(r1, 0);
		Arrays.fill(r2, 0);
		Arrays.fill(o1, 0);
		Arrays.fill(o2, 0);
		put(r1);
		put(r2);
		put(o1);
		put(o2);
		stateVariablesStale = false;
	}

	@Override
	public void run() {
		int synapseID = getGlobalId();
		if (synapseID >= size)
			return;
		int configID = componentConfigIndexes[synapseID];
		int preID = preIndexes[synapseID];
		int postID = postIndexes[synapseID];
		boolean preSpiked = neuronSpikings[preID];
		boolean postSpiked = neuronSpikings[postID];

		// Trace decays.
		r1[synapseID] -= r1[synapseID] * tPDecayMult[configID];
		r2[synapseID] -= r2[synapseID] * tXDecayMult[configID];
		o1[synapseID] -= o1[synapseID] * tNDecayMult[configID];
		o2[synapseID] -= o2[synapseID] * tYDecayMult[configID];

		// Need pre-spike values for these traces for strength update rules.
		double r2p = r2[synapseID];
		double o2p = o2[synapseID];

		if (preSpiked) {
			r1[synapseID] = 1;
			r2[synapseID] = 1;
		}
		if (postSpiked) {
			o1[synapseID] = 1;
			o2[synapseID] = 1;
		}

		if (preSpiked) {
			efficacy[synapseID] -= o1[synapseID] * (a2N[configID] + a3N[configID] * r2p);
		}
		if (postSpiked) {
			efficacy[synapseID] += r1[synapseID] * (a2P[configID] + a3P[configID] * o2p);
		}

		super.run();
	}

	@Override
	public boolean isNotUsed(int synapseIndex) {
		int configID = componentConfigIndexes[synapseIndex];
		return initialEfficacy[synapseIndex] == 0 && a2N[configID] == 0 && a3N[configID] == 0 && a2P[configID] == 0 && a3P[configID] == 0;
	}

	@Override
	public String[] getStateVariableNames() {
		String[] names = { "efficacy", "r1", "r2", "o1", "o2", };
		return names;
	}

	@Override
	public double[] getStateVariableValues(int synapseIndex) {
		ensureStateVariablesAreFresh();
		double[] values = { efficacy[synapseIndex], r1[synapseIndex], r2[synapseIndex], o1[synapseIndex], o2[synapseIndex] };
		return values;
	}

	@Override
	public void ensureStateVariablesAreFresh() {
		if (stateVariablesStale) {
			get(r1).get(r2).get(o1).get(o2);
		}
		super.ensureStateVariablesAreFresh();
	}

	@Override
	public Pfister2006SynapseConfiguration getConfigSingleton() {
		return configSingleton;
	}

	@Override
	public ComponentCollection createCollection(int size) {
		return new Pfister2006SynapseCollection(size);
	}
}
