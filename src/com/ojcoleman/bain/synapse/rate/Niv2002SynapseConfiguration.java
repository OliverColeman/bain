package com.ojcoleman.bain.synapse.rate;

import com.ojcoleman.bain.base.ComponentConfiguration;
import com.ojcoleman.bain.base.SynapseConfiguration;

/**
 * Configuration object for {@link Niv2002SynapseCollection}.
 * 
 * @author Oliver J. Coleman
 */
public class Niv2002SynapseConfiguration extends SynapseConfiguration {
	// Plasticity model parameters
	public double n, a, b, c, d;

	static String[] parameterLabels = {"n", "a", "b", "c", "d" };

	public Niv2002SynapseConfiguration() {
	}

	public Niv2002SynapseConfiguration(double[] params) {
		setParameterValues(params, false); // calls init()
	}

	public Niv2002SynapseConfiguration(String name, double[] params) {
		this.name = name;
		setParameterValues(params, false); // calls init()
	}

	public String[] getParameterNames() {
		return parameterLabels;
	}
	
	@Override
	public Niv2002SynapseConfiguration createConfiguration() {
		return new Niv2002SynapseConfiguration();
	}

	@Override
	public String[] getPresetNames() {
		return null;
	}

	@Override
	public ComponentConfiguration getPreset(int index) {
		return null;
	}
}
