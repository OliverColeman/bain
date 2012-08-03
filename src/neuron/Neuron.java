package neuron;

import base.ConfigurableComponent;

/**
 * Base class for all Neuron models.
 * @author Oliver J. Coleman
 */
public abstract class Neuron extends ConfigurableComponent {
	/**
	 * Refers to a pre-synaptic Neuron or spike.
	 */
	public static final int PRE = 0;
	
	/**
	 * Refers to a post-synaptic Neuron or spike.
	 */
	public static final int POST = 1;
	
	protected double output;
	  
	/**
	 * Returns true iff the Neuron spiked in the last time step.
	 */
	public abstract boolean spiked();
	
	/** 
	 * Returns the output of the Neuron for the last time step.
	 */
	public double getOutput() {
		return output;
	}
	
	/**
	 * Reset the model to its initial state.
	 */
	public void reset() {
		output = 0;
	}
}