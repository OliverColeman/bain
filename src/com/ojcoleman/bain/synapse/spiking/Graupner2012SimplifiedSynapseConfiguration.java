package com.ojcoleman.bain.synapse.spiking;

import com.ojcoleman.bain.base.SynapseConfiguration;

/**
 * Configuration object for {@link Graupner2012SynapseCollection}.
 * 
 * @author Oliver J. Coleman
 */
public class Graupner2012SimplifiedSynapseConfiguration extends SynapseConfiguration {
	// Calcium model parameters.
	public double tCDecay, cSpikePre, cSpikePost, cSpikePreDelay;
	// Plasticity model parameters
	public double depThresh, potThresh, depRate, potRate, w0, w1, initialP, timeScale, wRange;

	static String[] parameterLabels = { "tCDecay", "cSpikePre", "cSpikePost", "depThresh", "potThresh", "depRate", "potRate", "timeScale", "cSpikePreDelay", "w0", "w1", "initialP" };
	static String[] presetNames = { "DP-curve", "DPD-curve", "DPD'-curve", "P-curve", "D-curve", "D'-curve" };
	// Values must match order in parameterLabels.
	static double[][] presetValues = { { 20, 1, 2, 1, 1.3, 200, 321.808, 150, 13.7, 0, 1, 0.5 }, // DP
			{ 20, 0.9, 0.9, 1, 1.3, 250, 550, 150, 4.6, 0, 1, 0.5 }, // DPD
			{ 20, 1, 2, 1, 2.5, 50, 600, 150, 2.2, 0, 1, 0.5 }, // DPD'
			{ 20, 2, 2, 1, 1.3, 160, 257.447, 150, 0, 0, 1, 0.5 }, // P
			{ 20, 0.6, 0.6, 1, 1.3, 500, 550, 150, 0, 0, 1, 0.5 }, // D
			{ 20, 1, 2, 1, 3.5, 60, 600, 150, 0, 0, 1, 0.5 }, // D'
	};

	public Graupner2012SimplifiedSynapseConfiguration() {
	}

	public Graupner2012SimplifiedSynapseConfiguration(double[] params) {
		setParameterValues(params, false); // calls init()
	}

	public Graupner2012SimplifiedSynapseConfiguration(String name, double[] params) {
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
	public Graupner2012SimplifiedSynapseConfiguration getPreset(int index) {
		return new Graupner2012SimplifiedSynapseConfiguration(presetNames[index], presetValues[index]);
	}

	@Override
	public Graupner2012SimplifiedSynapseConfiguration createConfiguration() {
		return new Graupner2012SimplifiedSynapseConfiguration();
	}
}
