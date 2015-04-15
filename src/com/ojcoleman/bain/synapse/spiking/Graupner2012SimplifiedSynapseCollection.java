package com.ojcoleman.bain.synapse.spiking;

import com.ojcoleman.bain.base.ComponentCollection;
import com.ojcoleman.bain.base.ComponentConfiguration;
import com.ojcoleman.bain.base.SynapseCollection;

/**
 * A simplified implementation of the model described by Graupner, M., Brunel, N.: Calcium-Based Plasticity Model Explains Sensitivity of Synaptic Changes to
 * Spike Pattern, Rate, and Dendritic Location. PNAS. 109, 3991–3996 (2012). (using the simplified calcium model).
 * 
 * This implementation removes the bistability and noise components.
 * 
 * @see Graupner2012SynapseConfiguration
 * 
 * @author Oliver J. Coleman
 */
public class Graupner2012SimplifiedSynapseCollection extends SynapseCollection<Graupner2012SimplifiedSynapseConfiguration> {
	private static final Graupner2012SimplifiedSynapseConfiguration configSingleton = new Graupner2012SimplifiedSynapseConfiguration();

	// State variables.
	double[] c; // Calcium concentration.
	double[] p; // Efficacy state.
	int[] preDelayCount; // Count down until calcium spike after pre-synaptic neuronal spike.
	boolean[] preSpikedLastTimeStep, postSpikedLastTimeStep; // true iff the pre/post-synaptic neuron was spiking during the last time step. These are used to
																// prevent counting a spike that lasts multiple time steps more than once.

	// Model parameters, see SynapseConfigurationGraupner2012.
	public double[] cSpikePre, cSpikePost, tCDecayMult, depThresh, potThresh, depRateMult, potRateMult, w0, wRange, stepPeriod;
	public int[] cSpikePreDelayStepCount;

	public Graupner2012SimplifiedSynapseCollection(int size) {
		this.size = size;
		init();
	}

	@Override
	public void init() {
		super.init();
		// Init state variables.
		if (c == null || c.length != size) {
			c = new double[size];
			p = new double[size];
			preDelayCount = new int[size];
			preSpikedLastTimeStep = new boolean[size];
			postSpikedLastTimeStep = new boolean[size];
		}
		// Init parameter arrays.
		if (cSpikePre == null || cSpikePre.length != configs.size()) {
			cSpikePre = new double[configs.size()];
			cSpikePost = new double[configs.size()];
			tCDecayMult = new double[configs.size()];
			depThresh = new double[configs.size()];
			potThresh = new double[configs.size()];
			depRateMult = new double[configs.size()];
			potRateMult = new double[configs.size()];
			w0 = new double[configs.size()];
			wRange = new double[configs.size()];
			cSpikePreDelayStepCount = new int[configs.size()];
			stepPeriod = new double[1];
		}

		if (network != null) {
			for (int c = 0; c < configs.size(); c++) {
				Graupner2012SimplifiedSynapseConfiguration config = configs.get(c);
				cSpikePre[c] = config.cSpikePre;
				cSpikePost[c] = config.cSpikePost;
				tCDecayMult[c] = (1.0 / config.tCDecay) / (network.getTimeResolution() / 1000.0);
				depThresh[c] = config.depThresh;
				potThresh[c] = config.potThresh;
				depRateMult[c] = config.depRate / (network.getTimeResolution() * config.timeScale);
				potRateMult[c] = config.potRate / (network.getTimeResolution() * config.timeScale);
				w0[c] = config.w0;
				wRange[c] = config.w1 - config.w0;
				cSpikePreDelayStepCount[c] = (int) Math.round(config.cSpikePreDelay * (network.getTimeResolution() / 1000.0));
				stepPeriod[0] = network.getStepPeriod();
			}
		}

		// Transfer data to Aparapi kernel.
		// setExplicit(true);
		put(c);
		put(p);
		put(preDelayCount);
		put(preSpikedLastTimeStep);
		put(postSpikedLastTimeStep);
		put(cSpikePre);
		put(cSpikePost);
		put(tCDecayMult);
		put(depThresh);
		put(potThresh);
		put(depRateMult);
		put(potRateMult);
		put(w0);
		put(wRange);
		put(cSpikePreDelayStepCount);
		put(stepPeriod);
		stateVariablesStale = false;
	}

	public void reset() {
		for (int s = 0; s < size; s++) {
			Graupner2012SimplifiedSynapseConfiguration config = configs.get(componentConfigIndexes[s]);
			p[s] = config.initialP;
			efficacy[s] = config.w0 + p[s] * config.wRange;
			c[s] = 0;
			preDelayCount[s] = 0;
			preSpikedLastTimeStep[s] = false;
			postSpikedLastTimeStep[s] = false;
		}
		put(c);
		put(p);
		put(preDelayCount);
		put(preSpikedLastTimeStep);
		put(postSpikedLastTimeStep);
		put(efficacy);
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

		// Calcium decay.
		c[synapseID] -= c[synapseID] * tCDecayMult[configID];

		// If a pre spike occurred (ignore if we're still counting down from a
		// previous spike, not ideal but more efficient).
		if (!preSpikedLastTimeStep[synapseID] && preSpiked && preDelayCount[synapseID] == 0) {
			preDelayCount[synapseID] = cSpikePreDelayStepCount[configID] + 1;
		}

		if (preDelayCount[synapseID] > 0) {
			preDelayCount[synapseID]--;
			// If it's time to release the delayed calcium spike after a
			// pre-synaptic neuronal spike.
			if (preDelayCount[synapseID] == 0) {
				c[synapseID] += cSpikePre[configID];
			}
		}

		// If a post spike occurred.
		if (!postSpikedLastTimeStep[synapseID] && postSpiked) {
			c[synapseID] += cSpikePost[configID];
		}

		// Update strength if necessary.
		if (c[synapseID] >= depThresh[configID] || c[synapseID] >= potThresh[configID]) {
			// Determine what the next calcium concentration will likely be, to allow proportional potentiation or depression if it crosses one of the
			// thresholds between this step and the next.
			double nextC = c[synapseID] - c[synapseID] * tCDecayMult[configID];

			if (c[synapseID] >= potThresh[configID]) {
				// If the next calcium decay will drop the calcium below the potentiation threshold, then apply the potentiation proportionately.
				double scaling = (nextC >= potThresh[configID]) ? 1 : ((c[synapseID] - potThresh[configID]) / (c[synapseID] - nextC));
				p[synapseID] += potRateMult[configID] * (1 - p[synapseID]) * scaling;
			}
			if (c[synapseID] >= depThresh[configID]) {
				// If the next calcium decay will drop the calcium below the depression threshold, then apply the depression proportionately.
				double scaling = (nextC >= depThresh[configID]) ? 1 : ((c[synapseID] - depThresh[configID]) / (c[synapseID] - nextC));
				p[synapseID] -= depRateMult[configID] * p[synapseID] * scaling;
			}
			efficacy[synapseID] = w0[configID] + p[synapseID] * wRange[configID];
		}

		preSpikedLastTimeStep[synapseID] = preSpiked;
		postSpikedLastTimeStep[synapseID] = postSpiked;

		super.run();
	}
	
	@Override
	public boolean isNotUsed(int synapseIndex) {
		return initialEfficacy[synapseIndex] == 0 && potRateMult[componentConfigIndexes[synapseIndex]] == 0 && depRateMult[componentConfigIndexes[synapseIndex]] == 0;
	}

	@Override
	public String[] getStateVariableNames() {
		String[] names = { "Calcium", "p", "\u03B8p", "\u03B8d" };
		return names;
	}

	@Override
	public double[] getStateVariableValues(int synapseIndex) {
		ensureStateVariablesAreFresh();
		Graupner2012SimplifiedSynapseConfiguration config = configs.get(componentConfigIndexes[synapseIndex]);
		double[] values = { c[synapseIndex], p[synapseIndex], config.potThresh, config.depThresh };
		return values;
	}

	@Override
	public ComponentConfiguration getConfigSingleton() {
		return configSingleton;
	}

	@Override
	public void ensureStateVariablesAreFresh() {
		if (stateVariablesStale) {
			get(c).get(p);
		}
		super.ensureStateVariablesAreFresh();
	}

	@Override
	public ComponentCollection createCollection(int size) {
		return new Graupner2012SimplifiedSynapseCollection(size);
	}
}
