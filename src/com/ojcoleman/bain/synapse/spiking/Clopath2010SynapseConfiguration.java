package com.ojcoleman.bain.synapse.spiking;

import com.ojcoleman.bain.base.SynapseConfiguration;

/**
 * Configuration object for {@link Clopath2010SynapseCollection}.
 * 
 * @author Oliver J. Coleman
 */
public class Clopath2010SynapseConfiguration extends SynapseConfiguration {
	// Plasticity model parameters
	public double thetaNeg, thetaPos, aLTD, aLTP, tauX, tauNeg, tauPos, wMin, wMax;

	static String[] parameterLabels = { "thetaNeg", "thetaPos", "aLTD", "aLTP", "tauX", "tauNeg", "tauPos" };
	static String[] presetNames = { "Visual cortex", "Somatosensory cortex", "Hippocampal" };
	// Values must match order in parameterLabels.
	static double[][] presetValues = {
			// Not sure about the values for aLTP, some confusion about parameters published in the paper.
			{ -0.0706, -0.0453, 14E-5, 8E-3, 15, 10, 7 }, { -0.0706, -0.0453, 21E-5, 30E-3, 30, 6, 5 }, { -0.041, -0.038, 38E-5, 2E-3, 16, 1, 1 }, };

	public Clopath2010SynapseConfiguration() {
	}

	public Clopath2010SynapseConfiguration(double[] params) {
		setParameterValues(params, false); // calls init()
	}

	public Clopath2010SynapseConfiguration(String name, double[] params) {
		this.name = name;
		setParameterValues(params, false); // calls init()
	}

	public String[] getParameterNames() {
		return parameterLabels;
	}

	public String[] getPresetNames() {
		return presetNames;
	}

	@Override
	public Clopath2010SynapseConfiguration getPreset(int index) {
		return new Clopath2010SynapseConfiguration(presetNames[index], presetValues[index]);
	}

	@Override
	public Clopath2010SynapseConfiguration createConfiguration() {
		return new Clopath2010SynapseConfiguration();
	}
}
