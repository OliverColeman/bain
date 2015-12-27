package com.ojcoleman.bain.synapse.rate;

import com.ojcoleman.bain.base.ComponentConfiguration;
import com.ojcoleman.bain.base.SynapseConfiguration;

/**
 * Configuration object for {@link RisiModulatorySynapseCollection}.
 * 
 * @author Oliver J. Coleman
 */
public class RisiModulatorySynapseConfiguration extends Niv2002SynapseConfiguration {
	/**
	 * Indicates that the connection is a modulatory connection instead of a regular connection.
	 */
	public boolean modulatory;

	static String[] parameterLabels = {"modulatory", "n", "a", "b", "c", "d" };

	public RisiModulatorySynapseConfiguration() {
	}

	public RisiModulatorySynapseConfiguration(double[] params) {
		setParameterValues(params, false); // calls init()
	}

	public RisiModulatorySynapseConfiguration(String name, double[] params) {
		this.name = name;
		setParameterValues(params, false); // calls init()
	}

	public String[] getParameterNames() {
		return parameterLabels;
	}
	
	@Override
	public RisiModulatorySynapseConfiguration createConfiguration() {
		return new RisiModulatorySynapseConfiguration();
	}

	@Override
	public String[] getPresetNames() {
		return new String[] {"Fixed"};
	}

	@Override
	public ComponentConfiguration getPreset(int index) {
		return new RisiModulatorySynapseConfiguration("Fixed", new double[] {0, 0, 0, 0, 0, 0});
	}
}
