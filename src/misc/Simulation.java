package misc;

import java.util.HashMap;
import java.util.Vector;

import synapse.Synapse;

import neuron.Neuron;

public class Simulation {
	static Simulation singleton = new Simulation(1000);
	
	long step;
	int timeResolution;
	
	Vector<Neuron> neurons;
	Vector<Synapse> synapses;
	
	/**
	 * Create a new simulation.
	 */
	public Simulation(int timeResolution) {
		step = 0;
		this.timeResolution = timeResolution;
		this.neurons = new Vector<Neuron>();
		this.synapses = new Vector<Synapse>();
	}
	
	/**
	 * Create a new simulation.
	 * Sets the time resolution of all given Neurons and Synapses to match that of this Simulation and resets them.
	 */
	public Simulation(Vector<Neuron> neurons, Vector<Synapse>synapses, int timeResolution) {
		step = 0;
		this.timeResolution = timeResolution;
		this.neurons = neurons;
		this.synapses = synapses;
		
		// Ensure all components are using the right time resolution.
		for (Neuron n: neurons) {
			n.getConfig().setTimeResolution(timeResolution);
			n.reset();
			
		}
		for (Synapse s: synapses) {
			s.getConfig().setTimeResolution(timeResolution);
			s.reset();
		}
	}
	
	/**
	 * Reset the simulation.
	 */
	public void reset() {
		for (Neuron n: neurons) {
			n.reset();
		}
		for (Synapse s: synapses) {
			s.reset();
		}
		step = 0;
	}
	
	/**
	 * Set the time resolution of the simulation, and reset it.
	 */
	public void setTimeResolution(int timeResolution) {
		this.timeResolution = timeResolution;
		for (Neuron n: neurons) {
			n.getConfig().setTimeResolution(timeResolution);
			
		}
		for (Synapse s: synapses) {
			s.getConfig().setTimeResolution(timeResolution);
		}
		reset();
	}
	
	/**
	 * Simulate one time step.
	 */
	public void step() {
		for (Neuron n: neurons) {
			n.step();
		}
		for (Synapse s: synapses) {
			s.step();
		}
		step++;
	}
	
	/**
	 * Returns the current simulation step.
	 */
	public long getStep() {
		return step;
	}
	
	/**
	 * Returns the current simulation time in seconds.
	 */
	public double getTime() {
		return (double) step / timeResolution;
	}
	
	/**
	 * Adds the given Neuron to this simulation. Sets the Neurons time resolution to match that of this Simulation, and resets the Neuron.
	 */
	public void addNeuron(Neuron n) {
		neurons.add(n);
		n.getConfig().setTimeResolution(timeResolution);
		n.reset();
	}
	
	/**
	 * Adds the given Synapse to this simulation. Sets the Synapses time resolution to match that of this Simulation, and Synapses the Neuron.
	 */
	public void addSynapse(Synapse s) {
		synapses.add(s);
		s.getConfig().setTimeResolution(timeResolution);
		s.reset();
	}
	
	
	public static void setSingleton(Simulation sim) {
		singleton = sim;
	}
	
	public static Simulation getSingleton() {
		return singleton;
	}
}