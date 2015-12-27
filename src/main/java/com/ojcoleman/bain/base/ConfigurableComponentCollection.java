package com.ojcoleman.bain.base;

import java.util.ArrayList;

/**
 * <p>
 * Base class for all collections of configurable neural network components. A ConfigurableComponentCollection may reference zero, one or more
 * {@link ComponentConfiguration}s by adding them via {@link #addConfiguration(ComponentConfiguration)}. Each component in the collection may use one of these
 * configurations, this is set via {@link #setComponentConfiguration(int componentIndex, int configurationIndex)}.
 * </p>
 * 
 * <p>
 * See {@link ComponentCollection} for details on implementing component collections.
 * </p>
 * 
 * @author Oliver J. Coleman
 */
public abstract class ConfigurableComponentCollection<C extends ComponentConfiguration> extends ComponentCollection implements ComponentConfigurationListener {
	/**
	 * The component configurations used by the components in this collection.
	 */
	protected ArrayList<C> configs = new ArrayList<C>();

	/**
	 * An index into configs to specify the configuration to use for each component.
	 */
	protected int[] componentConfigIndexes;

	@Override
	public void init() {
		super.init();
		if (componentConfigIndexes == null || componentConfigIndexes.length != size) {
			componentConfigIndexes = new int[size];
		}
		// In case explicit mode is being used for the Aparapi kernel.
		put(componentConfigIndexes);
	}

	/**
	 * Add the specified configuration to the list of known configurations.
	 */
	public void addConfiguration(ComponentConfiguration componentConfiguration) {
		configs.add((C) componentConfiguration);
		componentConfiguration.addListener(this);
		init();
	}

	/**
	 * Replace the specified configuration in the list of known configurations.
	 * 
	 * @param configurationIndex The index of the configuration to replace.
	 * @param configurations The new configuration.
	 */
	public void setConfiguration(int configurationIndex, ComponentConfiguration configurations) {
		C config = (C) configurations; // Make sure it's the right type.
		if (configurationIndex < 0 || configurationIndex >= configs.size()) {
			throw new IllegalArgumentException("No configuration exists at index " + configurationIndex);
		}
		configs.get(configurationIndex).removeListener(this);
		configs.set(configurationIndex, config);
		configurations.addListener(this);
		init();
	}

	/**
	 * Get the specified configuration.
	 * 
	 * @param configurationIndex The index of the configuration to get.
	 */
	public C getConfiguration(int configurationIndex) {
		return configs.get(configurationIndex);
	}

	/**
	 * Set the configuration for a component. Parameterised implementations of a component should be accompanied by a configuration class extending
	 * ComponentConfiguration that allows specifying these parameters.
	 */
	public void setComponentConfiguration(int componentIndex, int configurationIndex) {
		componentConfigIndexes[componentIndex] = configurationIndex;
		// In case explicit mode is being used for the Aparapi kernel.
		put(componentConfigIndexes);
	}

	/**
	 * Get the number of configurations added to this collection.
	 */
	public int getConfigurationCount() {
		return configs.size();
	}

	/**
	 * Get the configuration for a component.
	 */
	public ComponentConfiguration getComponentConfiguration(int componentIndex) {
		return configs.get(componentConfigIndexes[componentIndex]);
	}
	
	/**
	 * Get the index of the configuration for the specified component.
	 */
	public int getComponentConfigurationIndex(int componentIndex) {
		return componentConfigIndexes[componentIndex];
	}
	
	/**
	 * Get a reference to the sub-class specific ComponentConfiguration object for this ConfigurableComponentCollection.
	 */
	public abstract ComponentConfiguration getConfigSingleton();

	/**
	 * This default implementation calls {@link #init()} and {@link #reset()}.
	 */
	public void configurationChanged(ComponentConfiguration c) {
		init();
		reset();
	}
}
