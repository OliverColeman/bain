package synapse;

import misc.ComponentConfiguration;
import misc.Simulation;

/**
 * Implementation of the synapse model described by
 * Graupner, M., Brunel, N.: Calcium-Based Plasticity Model Explains Sensitivity of Synaptic Changes to Spike Pattern, Rate, and Dendritic Location. PNAS. 109, 3991–3996 (2012).
 * (using the simplified calcium model).
 *
 * @author Oliver J. Coleman
 */
public class SynapseGraupner2012 extends Synapse {
	private static final SynapseGraupner2012Config configSingleton = new SynapseGraupner2012Config();
	
	SynapseGraupner2012Config config;
	
	double c = 0; // Calcium concentration.
	double p; // Efficacy state. 
	int preDelayCount = 0; //Count down until calcium spike after pre-synaptic neuronal spike. 
	
	public SynapseGraupner2012() {
	}
	public SynapseGraupner2012(SynapseGraupner2012Config config) {
		this.config = config;
	}
	
	public void setConfig(ComponentConfiguration config) {
		this.config = (SynapseGraupner2012Config) config;
	}
	public ComponentConfiguration getConfig() {
		return config;
	}
			    
	public void reset() {
		p = config.initialP;
		strength = config.w0 + p * config.wRange;
		c = 0;
		preDelayCount = 0;
	}
	
	public void reset(double initCalcium, double initEfficacy) {
		p = initEfficacy;
		strength = config.w0 + p * config.wRange;
		c = initCalcium;
		preDelayCount = 0;
	}
	
	public double step() {
	    // Calcium decay.
	    c -= c * config.tCDecayMult;

		// If a pre spike occurred (ignore if we're still counting down from a previous spike, not ideal but more efficient).
	    if (pre.spiked() && preDelayCount == 0) {
	    	preDelayCount = config.cSpikePreDelayStepCount+1;
	    }
	    
	    if (preDelayCount > 0) {
	    	preDelayCount--;
	    	// If it's time to release the delayed calcium spike after a pre-synaptic neuronal spike.
			if (preDelayCount == 0) {
	    		c += config.cSpikePre;
	    	}
		}
	    
	    
	    // If a post spike occurred.
	    if (post.spiked()) {
	        c += config.cSpikePost;
	    }
	    
	    // Update strength.
	    double delta_s = (-p * (1 - p) * (config.bistableBoundary - p)) * config.getStepPeriod(); // Multiply by inverse of time resolution.
	    if (c >= config.depThresh || c >= config.potThresh) {
		    if (c >= config.potThresh)
		    	delta_s += config.potRateMult * (1 - p);
		    if (c >= config.depThresh)
		    	delta_s -= config.depRateMult * p;
		    delta_s += config.noiseMult * config.rng.nextGaussian();
	    }
	    
	    p += delta_s * config.timeScaleInv;
	    if (p > 1) p = 1;
	    if (p < 0) p = 0;
	    strength = config.w0 + p * config.wRange;
	    	    
	    return pre.getOutput() * strength;
	}
	
	public double getCalciumConcentration() {
		return c;
	}

	@Override
	public String[] getStateVariableNames() {
		String[] names = {"Calcium", "p", "\u03B8p", "\u03B8d"};
		return names;
	}

	@Override
	public double[] getStateVariableValues() {
		double[] values = {c, p, config.potThresh, config.depThresh};
		return values;
	}
	
	@Override
	public ComponentConfiguration getConfigSingleton() {
		return configSingleton;
	}
	

}
