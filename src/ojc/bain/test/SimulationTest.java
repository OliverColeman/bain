package ojc.bain.test;

import java.util.Arrays;

import com.amd.aparapi.Kernel;

import ojc.bain.Simulation;
import ojc.bain.neuron.LinearNeuronCollection;
import ojc.bain.synapse.FixedSynapseCollection;

public class SimulationTest {
	/**
	 * Tests the core framework by creating a simple neural network from a {@link ojc.bain.neuron.LinearNeuronCollection} and
	 * {@link ojc.bain.synapse.FixedSynapseCollection} that tests communication between neurons and synapses over several
	 * simulation steps, using standard and OpenCL execution platforms if available.
	 * 
	 * @param mode The Aprarapi execution mode to use.
	 */
	public static double[] testCoreFramework(Kernel.EXECUTION_MODE mode) {
		LinearNeuronCollection neurons = new LinearNeuronCollection(9);
		FixedSynapseCollection synapses = new FixedSynapseCollection(10);
		Simulation sim = new Simulation(1000, neurons, synapses, mode);

		int[][] connections = new int[][] { { 0, 2 }, { 1, 2 }, { 1, 3 }, { 1, 4 }, { 3, 3 }, { 4, 5 }, { 5, 4 }, { 2, 6 }, { 3, 7 }, { 4, 8 } };
		double[] weights = new double[] { 1.0, 0.9, 1.0, 1.0, 0.5, 1.0, 1.0, 1.0, 1.0, 1.0 };
		double[] efficacy = synapses.getEfficacies();
		for (int s = 0; s < connections.length; s++) {
			synapses.setPreAndPostNeurons(s, connections[s][0], connections[s][1]);
			efficacy[s] = weights[s];
		}
		synapses.reset(); // put()s the efficacy array to the GPU if being used.

		// Initial spikes to two input neurons.
		neurons.addInput(0, 1);
		neurons.addInput(1, 1);

		for (int step = 0; step < 10; step++) {
			if (step == 4) {
				neurons.addInput(0, 1);
			}
			sim.step();
			System.out.println(Arrays.toString(neurons.getOutputs()));
		}
		System.out.println("Execution mode: " + neurons.getExecutionMode());
		System.out.println((neurons.isExplicit() ? "Explicit" : "Auto") + " memory management");
		System.out.println();

		return neurons.getOutputs();
	}
	
	public static void main(String[] args) {
		testCoreFramework(Kernel.EXECUTION_MODE.GPU);
	}
}
