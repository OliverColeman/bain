package ojc.bain;

import java.text.DecimalFormat;
import java.text.Format;

import com.amd.aparapi.Kernel;

import ojc.bain.NeuralNetwork;
import ojc.bain.neuron.spiking.FixedFrequencyNeuronCollection;
import ojc.bain.neuron.spiking.FixedFrequencyNeuronConfiguration;
import ojc.bain.synapse.rate.FixedSynapseCollection;
import ojc.bain.synapse.spiking.Pfister2006SynapseCollection;

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

		NeuralNetwork sim = new NeuralNetwork(1000, neurons, synapses, mode);

		for (int s = 0; s < synapseCount; s++) {
			synapses.setPreAndPostNeurons(s, (int) (Math.random() * neuronCount), (int) (Math.random() * neuronCount));
		}

		// Dry run.
		sim.run(100);
		if (neurons.getExecutionMode() != mode || synapses.getExecutionMode() != mode) {
			return -1;
		}

		// For real.
		sim.reset();
		long start = System.currentTimeMillis();
		sim.run(steps);
		long finish = System.currentTimeMillis();

		return finish - start;
	}

	public static void main(String[] args) {
		int steps = 1000;
		int timeResolution = 1000;
		int synapsesToNeuronsRatio = 1;
		Format format = new DecimalFormat("###############0.#");

		System.out.println("size    \tSEQ\tJTP\tGPU\tSEQ/JTP\tSEQ/GPU\tJTP/GPU");
		for (int size = 1024; size <= 1048576 * 2; size *= 2) {
			System.out.print(size + "    \t");
			try {
				long s = testFrameworkPerformance(size, size * synapsesToNeuronsRatio, timeResolution, steps, Kernel.EXECUTION_MODE.SEQ);
				System.out.print(format.format(s / 1000.0) + "\t");
				long j = testFrameworkPerformance(size, size * synapsesToNeuronsRatio, timeResolution, steps, Kernel.EXECUTION_MODE.JTP);
				System.out.print(format.format(j / 1000.0) + "\t");
				// System.out.print(" \t");
				long g = testFrameworkPerformance(size, size * synapsesToNeuronsRatio, timeResolution, steps, Kernel.EXECUTION_MODE.GPU);
				System.out.print(format.format(g / 1000.0) + "\t");
				System.out.print(format.format((double) s / j) + "\t");
				// System.out.print(" \t");
				System.out.print(format.format((double) s / g) + "\t");
				System.out.print(format.format((double) j / g) + "\t");
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println();
		}
	}
}
