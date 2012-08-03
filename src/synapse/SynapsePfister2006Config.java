package synapse;

import base.ComponentConfiguration;

/**
 * Configuration object for SynapsePfister2006.
 * 
 * @author Oliver J. Coleman
 */
public class SynapsePfister2006Config extends ComponentConfiguration {
	// Plasticity model parameters.
	public double tPDecay, tXDecay, tNDecay, tYDecay, a2N, a2P, a3N, a3P;
	double tPDecayMult, tXDecayMult, tNDecayMult, tYDecayMult, a2NMult, a2PMult, a3NMult, a3PMult;
	
	// Must match all parameter variable names.
	static String[] parameterLabels = {"tPDecay", "tXDecay", "tNDecay", "tYDecay", "a2N", "a2P", "a3N", "a3P"};
	static String[] presetNames = {"Hippocampal culture data set - Nearest spike - Min"};
	// Values must match order in parameterLabels.
	static double[][] presetValues = {
		{16.8, 1, 33.7, 48.0, 0.003, 0.0046, 0, 0.0091}
	};
	
	public SynapsePfister2006Config() {
	}
	
	public SynapsePfister2006Config(double[] params) {
		setParameterValues(params);
	}
	
	@Override
	public void init() {
		tPDecayMult = (1000 / tPDecay) / timeResolution;
		tXDecayMult = (1000 / tXDecay) / timeResolution;
		tNDecayMult = (1000 / tNDecay) / timeResolution;
		tYDecayMult = (1000 / tYDecay) / timeResolution;
		a2NMult = (1000 * a2N) / timeResolution;
		a2PMult = (1000 * a2P) / timeResolution;
		a3NMult = (1000 * a3N) / timeResolution;
		a3PMult = (1000 * a3P) / timeResolution;
	}
	
	public String[] getParameterNames() {
		return parameterLabels;
	}
	
	public String[] getPresetNames() {
		return presetNames;
	}
	
	public ComponentConfiguration getPreset(int index) {
		return new SynapsePfister2006Config(presetValues[index]);
	}
}