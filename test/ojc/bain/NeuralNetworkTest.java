package ojc.bain;

import java.util.Arrays;

import com.amd.aparapi.Kernel;
import com.ojcoleman.bain.NeuralNetwork;
import com.ojcoleman.bain.base.NeuronCollection;
import com.ojcoleman.bain.neuron.rate.LinearNeuronCollection;
import com.ojcoleman.bain.synapse.rate.FixedSynapseCollection;


import org.junit.*;
import static org.junit.Assert.*;


/**
 * JUnit tests  to test the core framework by creating a simple neural network from a {@link com.ojcoleman.bain.neuron.rate.LinearNeuronCollection} and
 * {@link com.ojcoleman.bain.synapse.rate.FixedSynapseCollection} that tests communication between neuron and synapse collections over several simulation steps, using all
 * Aparapi execution modes.
 */
public class NeuralNetworkTest {
	NeuralNetwork sim;
	double[] correctOutput;
	
	@Before
	public void setUp() {
		LinearNeuronCollection neurons = new LinearNeuronCollection(9);
		FixedSynapseCollection synapses = new FixedSynapseCollection(10);

		int[][] connections = new int[][] { { 0, 2 }, { 1, 2 }, { 1, 3 }, { 1, 4 }, { 3, 3 }, { 4, 5 }, { 5, 4 }, { 2, 6 }, { 3, 7 }, { 4, 8 } };
		double[] weights = new double[] { 1.0, 0.9, 1.0, 1.0, 0.5, 1.0, 1.0, 1.0, 1.0, 1.0 };
		double[] efficacy = synapses.getEfficacies();
		for (int s = 0; s < connections.length; s++) {
			synapses.setPreAndPostNeurons(s, connections[s][0], connections[s][1]);
			efficacy[s] = weights[s];
		}
		synapses.setEfficaciesModified();
		
		sim = new NeuralNetwork(1000, neurons, synapses);
		
		correctOutput = new double[]{0.0, 0.0, 0.0, 0.03125, 0.0, 1.0, 0.0, 0.0625, 1.0};
	}
	
	@Test
	public void testSEQ() {
		sim.setPreferredExecutionMode(Kernel.EXECUTION_MODE.SEQ);
		runSim(sim, false);
		assertTrue(Arrays.equals(sim.getNeurons().getOutputs(), correctOutput));
	}

	@Test
	public void testCPU() {
		sim.setPreferredExecutionMode(Kernel.EXECUTION_MODE.CPU);
		runSim(sim, false);
		assertTrue(Arrays.equals(sim.getNeurons().getOutputs(), correctOutput));
	}
	
	@Test
	public void testJTP() {
		sim.setPreferredExecutionMode(Kernel.EXECUTION_MODE.JTP);
		runSim(sim, false);
		assertTrue(Arrays.equals(sim.getNeurons().getOutputs(), correctOutput));
	}
	
	@Test
	public void testGPU() {
		sim.setPreferredExecutionMode(Kernel.EXECUTION_MODE.GPU);
		runSim(sim, false);
		assertTrue(Arrays.equals(sim.getNeurons().getOutputs(), correctOutput));
	}
	
	@After
	public void tearDown() {
		sim = null;
		correctOutput = null;
	}
	
	public static void runSim(NeuralNetwork sim, boolean printOutput) {
		NeuronCollection neurons = sim.getNeurons();
		
		// Initial spikes to two input neurons.
		neurons.setOutput(0, 1);
		neurons.setOutput(1, 1);

		for (int step = 0; step < 6; step++) {
			if (step == 3) {
				neurons.setOutput(0, 1);
			}
			sim.step();
			if (printOutput) {
				System.out.println(Arrays.toString(neurons.getOutputs()));
			}
		}
		
		if (printOutput) {
			System.out.println("Execution mode: " + neurons.getExecutionMode());
			System.out.println((neurons.isExplicit() ? "Explicit" : "Auto") + " memory management");
			System.out.println();
		}
	}

	
	public static void main(String[] args) {
		NeuralNetworkTest test = new NeuralNetworkTest();
		test.setUp();
		test.sim.setPreferredExecutionMode(Kernel.EXECUTION_MODE.CPU);
		runSim(test.sim, true);
		System.out.println(Arrays.equals(test.sim.getNeurons().getOutputs(), test.correctOutput) ? "Pass" : "Fail");
	}
}
