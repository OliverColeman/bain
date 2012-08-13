package ojc.bain.synapse;

import ojc.bain.base.ComponentConfiguration;

/**
 * Configuration object for {@link Pfister2006SynapseCollection}.
 * 
 * @author Oliver J. Coleman
 */
public class Pfister2006SynapseConfiguration extends ComponentConfiguration {
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
		setParameterValues(params);
	}

	public String[] getParameterNames() {
		return parameterLabels;
	}

	public String[] getPresetNames() {
		return presetNames;
	}

	public ComponentConfiguration getPreset(int index) {
		return new Pfister2006SynapseConfiguration(presetValues[index]);
	}

	@Override
	public ComponentConfiguration createConfiguration() {
		return new Pfister2006SynapseConfiguration();
	}
}