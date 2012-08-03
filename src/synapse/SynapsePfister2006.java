package synapse;

import misc.ComponentConfiguration;

/**
 * Implementation of the synapse model described by
 * Pfister, J.-P., Gerstner, W.: Triplets of Spikes in a Model of Spike Timing-Dependent Plasticity. J. Neurosci. 26, 9673–9682 (2006).
 *
 * @author Oliver J. Coleman
 */
public class SynapsePfister2006 extends Synapse {
	private static final SynapsePfister2006Config configSingleton = new SynapsePfister2006Config();
		
	public SynapsePfister2006Config config; 
	
	double r1 = 0, r2 = 0, o1 = 0, o2 = 0, r2p, o2p; // Spike traces.
	
	public SynapsePfister2006() {
	}
	public SynapsePfister2006(ComponentConfiguration config) {
		this.config = (SynapsePfister2006Config) config;
	}
	
	public void setConfig(ComponentConfiguration config) {
		this.config = (SynapsePfister2006Config) config;
	}
	public ComponentConfiguration getConfig() {
		return config;
	}
			    
	public void reset() {
		strength = 0;
		r1 = 0;
		r2 = 0;
		o1 = 0;
		o2 = 0;
	}
	
	public double step() {
		// Trace decays.
	    r1 -= r1 * config.tPDecayMult;
	    r2 -= r2 * config.tXDecayMult;
	    o1 -= o1 * config.tNDecayMult;
	    o2 -= o2 * config.tYDecayMult;
	    
	    // Need pre-spike values for these traces for strength update rules.
	    r2p = r2;
	    o2p = o2;
	    
	    if (pre.spiked())
	    	r1 = r2 = 1;
	    if (post.spiked()) 
	        o1 = o2 = 1;
	    
	    if (pre.spiked()) {
	    	strength -= o1 * (config.a2N + config.a3N * r2p);
	    }
	    if (post.spiked()) {
	        strength += r1 * (config.a2P + config.a3P * o2p);
	    }
	    
	    //System.out.println(r1 + "\t" + r2 + "\t" + o1 + "\t" + o2 + "\t" + strength);   
	    
	    return pre.getOutput() * strength;
	}
	
	
	@Override
	public String[] getStateVariableNames() {
		String[] names = {"r1", "r2", "o1", "o2", };
		return names;
	}
	
	@Override
	public double[] getStateVariableValues() {
		double[] values = {r1, r2, o1, o2};
		return values;
	}
	
	@Override
	public ComponentConfiguration getConfigSingleton() {
		return configSingleton;
	}
}
