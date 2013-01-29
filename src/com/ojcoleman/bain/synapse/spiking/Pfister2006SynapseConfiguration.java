package com.ojcoleman.bain.synapse.spiking;

import com.ojcoleman.bain.base.SynapseConfiguration;

/**
 * Configuration object for {@link Pfister2006SynapseCollection}.
 * 
 * @author Oliver J. Coleman
 */
public class Pfister2006SynapseConfiguration extends SynapseConfiguration {
	// Plasticity model parameters.
	public double tPDecay, tXDecay, tNDecay, tYDecay, a2N, a2P, a3N, a3P;

	// Must match all parameter variable names.
	static String[] parameterLabels = { "tPDecay", "tXDecay", "tNDecay", "tYDecay", "a2N", "a2P", "a3N", "a3P" };
	static String[] presetNames = { "Hippocampal culture data set - Nearest spike - Min" };
	// Values must match order in parameterLabels.
	static double[][] presetValues = { { 16.8, 1, 33.7, 48.0, 0.003, 0.0046, 0, 0.0091 } };

	public Pfister2006SynapseConfiguration() {
	}

	public Pfister2006SynapseConfiguration(double[] params) {
		setParameterValues(params, false);
	}

	public Pfister2006SynapseConfiguration(String name, double[] params) {
		this.name = name;
		setParameterValues(params, false);
	}

	@Override
	public String[] getParameterNames() {
		return parameterLabels;
	}

	@Override
	public String[] getPresetNames() {
		return presetNames;
	}

	@Override
	public Pfister2006SynapseConfiguration getPreset(int index) {
		return new Pfister2006SynapseConfiguration(presetNames[index], presetValues[index]);
	}

	@Override
	public Pfister2006SynapseConfiguration createConfiguration() {
		return new Pfister2006SynapseConfiguration();
	}
}