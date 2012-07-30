package neuron;

/**
 * Base class for all Neuron models.
 * @author Oliver J. Coleman
 */
public abstract class Neuron {
	protected double output;
	  
	/**
	 * Returns true iff the Neuron spiked in the last time step.
	 */
	public abstract boolean spiked();
	
	/**
	 * Update the model over one millisecond.
	 * @return The output value at the end of the specified time period.
	 */
	public abstract double step();
	  
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