package synapse;

import java.util.HashMap;
import java.util.Vector;

import neuron.Neuron;


/**
 * Base class for all Synapse models.
 * @author Oliver J. Coleman
 */
public abstract class Synapse {
	private static HashMap<String, Synapse> typeSingletons = new HashMap<String, Synapse>();
	
	protected double strength;
	protected Neuron pre, post;
	
	/**
	 * Set the configuration for the Synapse.
	 * By convention parameterised implementations of Synapse should be accompanied by a configuration class extending 
	 * SynapseConfig called <Synapse class name>Config that allows specifying these parameters.
	 */
	public abstract void setConfig(SynapseConfig config);
	
	/**
	 * Get the configuration for the Synapse.
	 * By convention parameterised implementations of Synapse should be accompanied by a configuration class extending 
	 * SynapseConfig called <Synapse class name>Config that allows specifying these parameters.
	 */
	public abstract SynapseConfig getConfig();
	
    /**
     * Update the model over one millisecond.
     * @return The output of the synapse at the end of the time step.
     */
    public abstract double step();
    
    /**
     * Reset the synapse model to its initial state.
     */
    public void reset() {
    	strength=0;
    }
    
    /**
     * Set the pre-synaptic Neuron.
     * @param pre The new pre-synaptic Neuron.
     */
    public void setPre(Neuron pre) {
		this.pre = pre;
	}
    
	/**
	 * Get the pre-synaptic Neuron.
	 * @return the pre-synaptic Neuron.
	 */
	public Neuron getPreNeuron() {
    	return pre;
    }
	
	/**
     * Set the post-synaptic Neuron.
     * @param pre The new post-synaptic Neuron.
     */
    public void setPost(Neuron post) {
		this.post = post;
	}

	/**
	 * Get the post-synaptic Neuron.
	 * @return the post-synaptic Neuron.
	 */
    public Neuron getPostNeuron() {
    	return post;
    }
    
    /**
	 * Get current strength (weight) value.
	 * @return the current strength (weight) value.
	 */
    public double getStrength() {
    	return strength;
    }
    
    /**
     * Get an array containing the names of all the internal state variables.
     * Used for testing or educational purposes.
     */
    public abstract String[] getStateVariableNames();
    
    /**
     * Get an array containing the values of all the internal state variables, in the same order as that given by getStateVariableNames().
     * Used for testing or educational purposes.
     */
    public abstract double[] getStateVariableValues();
    
    /**
     * Get a reference to the sub-class specific SynapseConfig object for this Synapse.
     */
    public abstract SynapseConfig getConfigSingleton();
    
    /**
     * Calling this method makes the specified Synapse type available in the list of Synapse types given by 
     * getSynapseTypes() and the getSynapseSingleton() method.
     * @param className The class of the Synapse type.
     * @throws ClassNotFoundException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */ 
    public static void registerSynapseType(String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
    	typeSingletons.put(className, ((Class<? extends Synapse>) Class.forName(className)).newInstance());
    }
    
    public static String[] getSynapseTypes() {
    	return typeSingletons.keySet().toArray(new String[typeSingletons.size()]);
    }
    
    public static Synapse getSynapseSingleton(String className) {
    	return typeSingletons.get(className);
    }
}
