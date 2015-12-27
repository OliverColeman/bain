package com.ojcoleman.bain.synapse.spiking;

import com.ojcoleman.bain.base.ComponentCollection;
import com.ojcoleman.bain.base.ComponentConfiguration;
import com.ojcoleman.bain.base.NeuronConfiguration;
import com.ojcoleman.bain.base.SynapseCollection;

/**
 * Implementation of the model described by Clopath et al.: "Connectivity reflects coding: a model of voltage-based STDP with homeostasis", (2010).
 * 
 * @see Clopath2010SynapseConfiguration
 * 
 * @author Oliver J. Coleman
 */
public class Clopath2010SynapseCollection extends SynapseCollection<Clopath2010SynapseConfiguration> {
	private static final Clopath2010SynapseConfiguration configSingleton = new Clopath2010SynapseConfiguration();

	// State variables.
	double[] uNeg, uPos; // Low-pass-filtered versions of the post-synaptic membrane potential (neuron output).
	double[] x; // Pre-synaptic trace.

	// Model parameters, see Clopath2010SynapseConfiguration.
	public double[] thetaNeg, thetaPos, aLTD, aLTPMult, tauXMult, tauNegMult, tauPosMult, stepPeriod, efficacyMin, efficacyMax;

	public Clopath2010SynapseCollection(int size) {
		this.size = size;
		init();
	}

	@Override
	public void init() {
		super.init();
		// Init state variables.
		if (uNeg == null || uNeg.length != size) {
			uNeg = new double[size];
			uPos = new double[size];
			x = new double[size];
		}
		// Init parameter arrays.
		if (thetaNeg == null || thetaNeg.length != configs.size()) {
			thetaNeg = new double[configs.size()];
			thetaPos = new double[configs.size()];
			aLTD = new double[configs.size()];
			aLTPMult = new double[configs.size()];
			tauXMult = new double[configs.size()];
			tauNegMult = new double[configs.size()];
			tauPosMult = new double[configs.size()];
			efficacyMin = new double[configs.size()];
			efficacyMax = new double[configs.size()];
			stepPeriod = new double[1];
		}

		if (network != null) {
			for (int c = 0; c < configs.size(); c++) {
				Clopath2010SynapseConfiguration config = configs.get(c);
				tauXMult[c] = (1.0 / config.tauX) / (network.getTimeResolution() / 1000.0);
				tauNegMult[c] = (1.0 / config.tauNeg) / (network.getTimeResolution() / 1000.0);
				tauPosMult[c] = (1.0 / config.tauPos) / (network.getTimeResolution() / 1000.0);
				thetaNeg[c] = config.thetaNeg;
				thetaPos[c] = config.thetaPos;
				aLTD[c] = config.aLTD;
				aLTPMult[c] = config.aLTP / (network.getTimeResolution() / 1000.0);
				efficacyMin[c] = config.minimumEfficacy;
				efficacyMax[c] = config.maximumEfficacy;
			}
			stepPeriod[0] = network.getStepPeriod();
		}

		// Transfer data to Aparapi kernel.
		// setExplicit(true);
		put(uNeg);
		put(uPos);
		put(x);
		put(thetaNeg);
		put(thetaPos);
		put(aLTD);
		put(aLTPMult);
		put(tauXMult);
		put(tauNegMult);
		put(tauPosMult);
		put(efficacyMin);
		put(efficacyMax);
		put(stepPeriod);
		stateVariablesStale = false;
	}

	public void reset() {
		super.reset();
		for (int s = 0; s < size; s++) {
			NeuronConfiguration neuronConfig = network.getNeurons().getComponentConfiguration(postIndexes[s]);
			uNeg[s] = neuronConfig.restPotential;
			uPos[s] = neuronConfig.restPotential;
			x[s] = 0;
		}
		put(uNeg);
		put(uPos);
		put(x);
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

		// Update trace variables/low pass filters.
		x[synapseID] += ((preSpiked ? 1 : 0) - x[synapseID]) * tauXMult[configID];
		uNeg[synapseID] += (neuronOutputs[postID] - uNeg[synapseID]) * tauNegMult[configID];
		uPos[synapseID] += (neuronOutputs[postID] - uPos[synapseID]) * tauPosMult[configID];

		// If LTD occurs.
		double uSigmaNeg = uNeg[synapseID] - thetaNeg[configID];
		if (preSpiked && uSigmaNeg > 0) {
			efficacy[synapseID] -= aLTD[configID] * uSigmaNeg;
			if (efficacy[synapseID] < efficacyMin[configID])
				efficacy[synapseID] = efficacyMin[configID];
		}
		// If LTP occurs.
		double uSigma = neuronOutputs[postID] - thetaPos[configID];
		double uSigmaPos = uPos[synapseID] - thetaNeg[configID];
		if (uSigma > 0 && uSigmaPos > 0) {
			efficacy[synapseID] += aLTPMult[configID] * x[synapseID] * uSigma * uSigmaPos;
			if (efficacy[synapseID] > efficacyMax[configID])
				efficacy[synapseID] = efficacyMax[configID];
		}

		super.run();
	}
	
	@Override
	public boolean isNotUsed(int synapseIndex) {
		return initialEfficacy[synapseIndex] == 0 && aLTD[componentConfigIndexes[synapseIndex]] == 0 && aLTPMult[componentConfigIndexes[synapseIndex]] == 0;
	}

	@Override
	public String[] getStateVariableNames() {
		String[] names = { "efficacy", "u-", "u+", "x", "\u03B8-", "\u03B8+" };
		return names;
	}

	@Override
	public double[] getStateVariableValues(int synapseIndex) {
		ensureStateVariablesAreFresh();
		Clopath2010SynapseConfiguration config = configs.get(componentConfigIndexes[synapseIndex]);
		double[] values = { efficacy[synapseIndex], uNeg[synapseIndex], uPos[synapseIndex], x[synapseIndex], config.thetaNeg, config.thetaPos };
		return values;
	}

	@Override
	public ComponentConfiguration getConfigSingleton() {
		return configSingleton;
	}

	@Override
	public void ensureStateVariablesAreFresh() {
		if (stateVariablesStale) {
			get(uNeg).get(uPos).get(x);
		}
		super.ensureStateVariablesAreFresh();
	}

	@Override
	public ComponentCollection createCollection(int size) {
		return new Clopath2010SynapseCollection(size);
	}
}
