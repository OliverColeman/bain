package synapse;

/**
 * Implementation of the synapse model described by
 * Graupner, M., Brunel, N.: Calcium-Based Plasticity Model Explains Sensitivity of Synaptic Changes to Spike Pattern, Rate, and Dendritic Location. PNAS. 109, 3991–3996 (2012).
 *
 * @author Oliver J. Coleman
 */
public class SynapseGraupner2012 extends Synapse {
	private static final SynapseGraupner2012Config configSingleton = new SynapseGraupner2012Config();
	
	SynapseGraupner2012Config config;
	
	double c = 0; // Calcium concentration.
	int preDelayCount = 0; //Count down until calcium spike after pre-synaptic neuronal spike. 
	
	public SynapseGraupner2012() {
	}
	public SynapseGraupner2012(SynapseGraupner2012Config config) {
		this.config = config;
	}
	
	public void setConfig(SynapseConfig config) {
		this.config = (SynapseGraupner2012Config) config;
	}
	public SynapseConfig getConfig() {
		return config;
	}
			    
	public void reset() {
		strength = 0;
		c = 0;
		preDelayCount = 0;
	}
	
	public double step() {
		if (preDelayCount > 0) {
	    	preDelayCount--;
	    	// If it's time to release the delayed calcium spike after a pre-synaptic neuronal spike.
			if (preDelayCount == 0) {
	    		c += config.cSpikePre;
	    	}
		}
	    	
		// If a pre spike occurred (ignore if we're still counting down from a previous spike).
	    if (pre.spiked() && preDelayCount == 0) {
	    	preDelayCount = (int) Math.round(config.cSpikePreDelay);
	    }
	    
	    // If a post spike occurred.
	    if (post.spiked())
	        c += config.cSpikePost;
	        
	    // Update strength.
	    double delta_s = -strength * (1 - strength) * (config.bistableBoundary - strength);
	    if (c > config.potThresh)
	    	delta_s += config.potRate * (1 - strength);
	    if (c > config.depThresh)
	    	delta_s -= config.depRate * strength;
	    //if (c > config.depThresh || c > config.potThresh)
	    //	delta_s += config.noiseRate * config.timeScaleSqrt * (double) config.rng.nextGaussian();
	    
	    strength += delta_s * config.timeScaleInv;
	    
	    // Calcium decay.
	    c -= c / config.tCDecay;
	    
	    return pre.getOutput() * strength;
	}
	
	public double getCalciumConcentration() {
		return c;
	}

	@Override
	public String[] getStateVariableNames() {
		String[] names = {"Calcium"};
		return names;
	}

	@Override
	public double[] getStateVariableValues() {
		double[] values = {c};
		return values;
	}
	
	@Override
	public SynapseConfig getConfigSingleton() {
		return configSingleton;
	}
}
