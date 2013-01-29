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
public abstract class SynapseConfiguration extends ComponentConfiguration {
	/**
	 * A synapses minimum efficacy. Default is 0;
	 */
	public double minimumEfficacy = 0;

	/**
	 * A synapses maximum efficacy. Default is 1;
	 */
	public double maximumEfficacy = 1;
}