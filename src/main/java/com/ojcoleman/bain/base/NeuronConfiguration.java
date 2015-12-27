package com.ojcoleman.bain.base;

import com.ojcoleman.bain.base.ComponentConfiguration;

/**
 * <p>
 * A base class for configuration objects for neurons.
 * </p>
 * 
 * @see ComponentConfiguration
 * 
 * @author Oliver J. Coleman
 */
public abstract class NeuronConfiguration extends ComponentConfiguration {
	/**
	 * A neurons potential (output voltage) during a spike. Default value is 30.4mv.
	 */
	public double spikePotential = 0.0304;

	/**
	 * A neurons potential between spikes. Default value is -70.6mv.
	 */
	public double restPotential = -0.0706;
}