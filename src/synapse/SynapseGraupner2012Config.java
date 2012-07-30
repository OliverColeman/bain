package synapse;

import misc.Ziggurat;

/**
 * Configuration object for SynapseGraupner2012.
 * 
 * @author Oliver J. Coleman
 */
public class SynapseGraupner2012Config extends SynapseConfig {
	// Calcium model parameters.
	public double tCDecay, cSpikePre, cSpikePost, cSpikePreDelay;
	// Plasticity model parameters
	public double depThresh, potThresh, depRate, potRate, noiseRate, bistableBoundary, timeScaleInv, timeScaleSqrt;
	
	static String[] parameterLabels = {"tCDecay", "cSpikePre", "cSpikePost", "cSpikePreDelay", "depThresh", "potThresh", "depRate", "potRate", "noiseRate", "bistableBoundary"};
	static String[] presetNames = {"spike pair DP"};
	// Values must match order in parameterLabels.
	static double[][] presetValues = {
		{20, 1, 2, 13.7, 1, 1.3, 200, 321.808, 2.8284, 0.5}
	};
	
	public Ziggurat rng;
	
	public SynapseGraupner2012Config() {
	}
	
	public SynapseGraupner2012Config(double[] params) {
		setParameterValues(params);
		_init(1, 123);
	}
	
	private void _init(double timeScale, long rngSeed) {
		this.timeScaleInv = (1.0 / timeScale) / 1000f;
		this.timeScaleSqrt = Math.sqrt(timeScale);
		rng = new Ziggurat(rngSeed);
	}
	
	public String[] getParameterNames() {
		return parameterLabels;
	}
	
	public String[] getPresetNames() {
		return presetNames;
	}
	
	public SynapseConfig getPreset(int index) {
		return new SynapseGraupner2012Config(presetValues[index]);
	}
}
