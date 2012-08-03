package synapse;

import misc.ConfigurableComponent;
import neuron.Neuron;


/**
 * Base class for all Synapse models.
 * @author Oliver J. Coleman
 */
public abstract class Synapse extends ConfigurableComponent {
	protected double strength;
	protected Neuron pre, post;
	
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
}
