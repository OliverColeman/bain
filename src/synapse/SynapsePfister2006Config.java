package synapse;

/**
 * Configuration object for SynapsePfister2006.
 * 
 * @author Oliver J. Coleman
 */
public class SynapsePfister2006Config extends SynapseConfig {
	// Plasticity model parameters.
	public double tPDecay, tXDecay, tNDecay, tYDecay, a2N, a2P, a3N, a3P;
	
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
	
	public String[] getParameterNames() {
		return parameterLabels;
	}
	
	public String[] getPresetNames() {
		return presetNames;
	}
	
	public SynapseConfig getPreset(int index) {
		return new SynapsePfister2006Config(presetValues[index]);
	}
}