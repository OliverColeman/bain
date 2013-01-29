package com.ojcoleman.bain.misc;

import java.text.DecimalFormat;
import java.text.Format;


import com.amd.aparapi.Kernel;
import com.ojcoleman.bain.NeuralNetwork;
import com.ojcoleman.bain.neuron.spiking.FixedFrequencyNeuronCollection;
import com.ojcoleman.bain.neuron.spiking.FixedFrequencyNeuronConfiguration;
import com.ojcoleman.bain.synapse.rate.FixedSynapseCollection;
import com.ojcoleman.bain.synapse.spiking.Pfister2006SynapseCollection;


public class PerformanceTest {
	/**
	 * Tests the performance of the framework on a randomly generated network consisting of a Pfister2006SynapseCollection and FixedFrequencyNeuronCollection.
	 * 
	 * @param neuronCount The number of neurons to simulate.
	 * @param synapseCount The number of synapses to simulate
	 * @param timeResolution The time resolution of the simulation (steps per simulation second).
	 * @param steps The number of simulation steps to run.
	 * @param mode The Aprarapi execution mode to use.
	 * @return The time taken in milliseconds, or -1 if unable to run simulation in requested execution mode.
	 */
	public static long testFrameworkPerformance(int neuronCount, int synapseCount, int timeResolution, int steps, Kernel.EXECUTION_MODE mode) {
		FixedFrequencyNeuronCollection neurons = new FixedFrequencyNeuronCollection(neuronCount);
		neurons.addConfiguration(new FixedFrequencyNeuronConfiguration(Math.random() * 0.98 + 0.02)); // Spiking period between 0.02 and 1s (1 to 50Hz).

		Pfister2006SynapseCollection synapses = new Pfister2006SynapseCollection(synapseCount);
		synapses.addConfiguration(synapses.getConfigSingleton().getPreset(0));
		///FixedSynapseCollection synapses = new FixedSynapseCollection(synapseCount);
		
		NeuralNetwork sim = new NeuralNetwork(timeResolution, neurons, synapses, mode);

		for (int s = 0; s < synapseCount; s++) {
			synapses.setPreAndPostNeurons(s, (int) (Math.random() * neuronCount), (int) (Math.random() * neuronCount));
			synapses.setEfficacy(s, Math.random());
		}
		synapses.setEfficaciesModified();

		// System.out.println("memory used: " + (Runtime.getRuntime().totalMemory() / (1024*1024)));

		// Dry run.
		sim.run(1000);
		if (neurons.getExecutionMode() != mode || synapses.getExecutionMode() != mode) {
			return -1;
		}

		// For real.
		long start = System.currentTimeMillis();
		sim.run(steps);
		long finish = System.currentTimeMillis();

		return finish - start;
	}

	public static void main(String[] args) {
		int steps = 5000;
		int timeResolution = 1000;
		int synapsesToNeuronsRatio = 16;
		Format format = new DecimalFormat("###############0.#");

		System.out.println("size    \tSEQ\tJTP\tGPU\tSEQ/JTP\tSEQ/GPU\tJTP/GPU");
		for (int size = 8; size <= 1048576 * 2; size *= 2) {
			int synapseCount = size * synapsesToNeuronsRatio;
			System.out.print(size + "/" + synapseCount + "   \t");
			try {
				long s = testFrameworkPerformance(size, synapseCount, timeResolution, steps, Kernel.EXECUTION_MODE.SEQ);
				System.out.print(format.format(s / 1000.0) + "\t");
				long j = testFrameworkPerformance(size, synapseCount, timeResolution, steps, Kernel.EXECUTION_MODE.JTP);
				System.out.print(format.format(j / 1000.0) + "\t");
				//System.out.print(" \t");
				long g = testFrameworkPerformance(size, synapseCount, timeResolution, steps, Kernel.EXECUTION_MODE.GPU);
				System.out.print(format.format(g / 1000.0) + "\t");
				System.out.print(format.format((double) s / j) + "\t");
				//System.out.print(" \t");
				System.out.print(format.format((double) s / g) + "\t");
				System.out.print(format.format((double) j / g) + "\t");
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println();
		}
	}
}