package synapse;

import base.ComponentConfiguration;
import misc.Ziggurat;

/**
 * Configuration object for SynapseGraupner2012.
 * 
 * @author Oliver J. Coleman
 */
public class SynapseGraupner2012Config extends ComponentConfiguration {
	// Calcium model parameters.
	public double tCDecay, cSpikePre, cSpikePost, cSpikePreDelay;
	// Plasticity model parameters
	public double depThresh, potThresh, depRate, potRate, noiseRate, bistableBoundary, w0, w1, initialP;
	
	public double tCDecayMult, timeScale, timeScaleInv, timeScaleSqrt, wRange, depRateMult, potRateMult, noiseMult;
	public int cSpikePreDelayStepCount;
		
	static String[] parameterLabels = {"tCDecay", "cSpikePre", "cSpikePost", "depThresh", "potThresh", "depRate", "potRate", "noiseRate", "timeScale", "bistableBoundary", "cSpikePreDelay", "w0", "w1", "initialP"};
	static String[] presetNames = {"DP-curve", "DPD-curve", "DPD'-curve", "P-curve", "D-curve", "D'-curve"};
	// Values must match order in parameterLabels.
	static double[][] presetValues = {
		{20,	1,		2,		1,		1.3, 	200, 		321.808, 	2.8284, 	150, 	0.5, 	13.7, 	0, 	1, 0.5}, //DP
		{20,	0.9,	0.9, 	1,		1.3, 	250, 		550, 		2.8284, 	150, 	0.5, 	4.6, 	0, 	1, 0.5}, //DPD
		{20,	1,		2, 		1,		2.5, 	50, 		600, 		2.8284, 	150, 	0.5, 	2.2, 	0, 	1, 0.5}, //DPD'
		{20,	2,		2,		1,		1.3, 	160, 		257.447, 	2.8284, 	150, 	0.5, 	0, 		0, 	1, 0.5}, //P
		{20,	0.6,	0.6,	1,		1.3, 	500, 		550,		5.6568, 	150, 	0.5, 	0, 		0, 	1, 0.5}, //D
		{20,	1,		2,		1,		3.5, 	60, 		600,		2.8284, 	150, 	0.5, 	0, 		0, 	1, 0.5}, //D'
	};
	
	public Ziggurat rng;
	
	public SynapseGraupner2012Config() {
		init();
		rng = new Ziggurat(123);
	}
	
	public SynapseGraupner2012Config(double[] params) {
		setParameterValues(params); //calls init()
		rng = new Ziggurat(123);
	}
	
	/**
	 * Initialise values generated from the parameters. 
	 * This method should be called if any parameters are changed directly (rather than via the setParameterValue method).
	 */
	@Override
	public void init() {
		tCDecayMult = (1.0 / tCDecay) / (timeResolution/1000.0);
		timeScaleInv = (1.0 / timeScale);
		timeScaleSqrt = Math.sqrt(timeScale);
		wRange = w1 - w0; 
		cSpikePreDelayStepCount = (int) Math.round(cSpikePreDelay * (timeResolution / 1000.0));
		depRateMult = depRate / timeResolution;
		potRateMult = potRate / timeResolution;
		noiseMult = (noiseRate * timeScaleSqrt) / (Math.sqrt(timeResolution)*10);
	}
	
	public String[] getParameterNames() {
		return parameterLabels;
	}
	
	public String[] getPresetNames() {
		return presetNames;
	}
	
	public ComponentConfiguration getPreset(int index) {
		return new SynapseGraupner2012Config(presetValues[index]);
	}
}
