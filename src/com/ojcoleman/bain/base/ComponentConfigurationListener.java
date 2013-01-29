package com.ojcoleman.bain.base;

/**
 * Interface for classes that are to be notified of changes to a {@link ComponentConfiguration}.
 * 
 * @author Oliver J. Coleman
 */
public interface ComponentConfigurationListener {
	/**
	 * Invoked upon changes to a ComponentConfiguration.
	 * 
	 * @param c The modified ComponentConfiguration.
	 */
	public void configurationChanged(ComponentConfiguration c);
}
