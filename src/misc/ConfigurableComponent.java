package misc;

import java.util.HashMap;

/**
 * Base class for all configurable neural network component models.
 * @author Oliver J. Coleman
 */
public abstract class ConfigurableComponent {
	private static HashMap<String, ConfigurableComponent> typeSingletons = new HashMap<String, ConfigurableComponent>();
	
	/**
	 * Set the configuration for the Synapse.
	 * By convention parameterised implementations of Synapse should be accompanied by a configuration class extending 
	 * SynapseConfig called <Synapse class name>Config that allows specifying these parameters.
	 */
	public abstract void setConfig(ComponentConfiguration config);
	
	/**
	 * Get the configuration for the Synapse.
	 * By convention parameterised implementations of Synapse should be accompanied by a configuration class extending 
	 * SynapseConfig called <Synapse class name>Config that allows specifying these parameters.
	 */
	public abstract ComponentConfiguration getConfig();
	
    /**
     * Update the model over one time step.
     * @return May be used to return a single useful scalar value (eg Synapse or Neuron output).
     */
    public abstract double step();
    
    /**
     * Reset the component to its initial state. 
     * Sub-classes can override this method if they have state variables that may be reset to an initial state. 
     */
    public void reset() {}
        
    /**
     * Get an array containing the names of all the internal state variables.
     * Sub-classes may override this to allow the component to be used for testing or educational purposes.
     */
    public String[] getStateVariableNames(){ return null; }
    
    /**
     * Get an array containing the values of all the internal state variables, in the same order as that given by getStateVariableNames().
     * Sub-classes may override this to allow the component to be used for testing or educational purposes.
     */
    public double[] getStateVariableValues(){ return null; }
    
    /**
     * Get a reference to the sub-class specific ComponentConfiguration object for this Synapse.
     */
    public abstract ComponentConfiguration getConfigSingleton();
    
    /**
     * Calling this method makes the specified Synapse type available in the list of Synapse types given by 
     * getSynapseTypes() and the getSynapseSingleton() method.
     * @param className The class of the Synapse type.
     * @throws ClassNotFoundException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */ 
    public static void registerComponentType(String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
    	typeSingletons.put(className, ((Class<? extends ConfigurableComponent>) Class.forName(className)).newInstance());
    }
    
    public static String[] getComponentTypes() {
    	return typeSingletons.keySet().toArray(new String[typeSingletons.size()]);
    }
    
    public static ConfigurableComponent getComponentSingleton(String className) {
    	return typeSingletons.get(className);
    }
}