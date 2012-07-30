package neuron;

/**
 * A neuron that produces randomised spike trains.
 * 
 * TODO: Allow different probability distributions, use more efficient RNG(s).
 * 
 * @author Oliver J. Coleman
 */
public class NeuronRandom extends Neuron {
	double threshold;
	
	/**
	 * Create a NeuronRandom with the given firing threshold. The threshold, which should be between 0 and 1, 
	 * describes how likely the Neuron is to spike at any given time step: a value of 0 would cause the Neuron
	 * to spike every time step, a value of 1 would cause the Neuron to never spike, a value of 0.5 would cause
	 * the Neuron to spike 50% of time steps.
	 * @param threshold The firing threshold.
	 */ 
	public NeuronRandom(double threshold) {
		this.threshold = threshold;
	}
	
	/**
	 * @see neuron.Neuron#spiked()
	 */
	public boolean spiked() {
		return output >= 1; 
	}
	
	
	/**
	 * @see neuron.Neuron#step()
	 */
	public double step() {
		output = (Math.random() > threshold) ? 1 : 0;
		return output;
	}
}