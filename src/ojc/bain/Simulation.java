package ojc.bain;

import java.util.Arrays;

import ojc.bain.base.*;
import ojc.bain.misc.*;
import ojc.bain.neuron.*;
import ojc.bain.synapse.*;
import ojc.bain.test.*;

import com.amd.aparapi.Kernel;

/**
 * <p>
 * The main class for running a neural network simulation. A Simulation is run at a specified time resolution, which is the
 * number of discrete simulation steps performed for each second of simulation time. A typical resolution is 1000, or 1ms
 * duration for each step.
 * </p>
 * 
 * <p>
 * A Simulation consists of a {@link ojc.bain.base.NeuronCollection} and a {@link ojc.bain.base.SynapseCollection}. These
 * collections of neural network components are designed to be executable on SIMD hardware (eg a GPU) via OpenCL via Aparapi
 * (See {@link ojc.bain.base.ComponentCollection} for more details).
 * </p>
 * 
 * <p>
 * To improve performance, ComponentCollection and extensions thereof use the explicit memory management feature of Aparapi.
 * When using SIMD hardware such as a GPU, the data to be processed must be transferred to the device, and the results
 * transferred back to the CPU after the computations have been performed. These transfers take a significant amount of time. To
 * minimise the amount of transfers to and from the SIMD hardware, NeuronCollection and SynapseCollection objects use shared
 * data structures on the SIMD hardware, thus we only transfer neuron and synapse state data to the SIMD hardware at the
 * beginning of a simulation, and input and output data when necessary (see {@link ojc.bain.base.ComponentCollection} for more
 * details). However, for this to work both the neuron and synapse models must be executing on the SIMD hardware. The Simulation
 * class ensures that the neuron and synapse models are either both executing on the SIMD hardware, or both on the CPU (we don't
 * allow for executing one on SIMD hardware and one on the CPU as this would require a lot of data transfer to and from the SIMD
 * hardware, usually negating performance gains from using the SIMD hardware). If performance is important then make sure your
 * model can be converted to OpenCL by Aparapi; a Simulation provides a warning if one or the other of a NeuronCollection or
 * SynapseCollection can not be converted to OpenCL. Finally, for networks consisting of fewer than 512 synapses, the Simulation
 * forces the use of the CPU as this is typically more performant than use of SIMD hardware. In the future the ability to
 * dynamically determine which combination of SIMD and/or CPU processing provides the most performance may be included.
 * 
 * @author Oliver J. Coleman
 */
public class Simulation {
	static Simulation singleton = new Simulation(1000);

	protected long step;
	protected int timeResolution = 1000;
	protected double stepPeriod = 1.0 / timeResolution;

	NeuronCollection<? extends ComponentConfiguration> neurons;
	SynapseCollection<? extends ComponentConfiguration> synapses;

	/**
	 * Create a new simulation.
	 */
	public Simulation(int timeResolution) {
		step = 0;
		this.timeResolution = timeResolution;
	}

	/**
	 * Create a new simulation. Sets the time resolution of all given Neurons and Synapses to match that of this Simulation and
	 * resets them.
	 */
	public Simulation(int timeResolution, NeuronCollection<? extends ComponentConfiguration> neurons, SynapseCollection<? extends ComponentConfiguration> synapses) {
		this.timeResolution = timeResolution;
		this.neurons = neurons;
		this.synapses = synapses;
		neurons.setSimulation(this);
		synapses.setSimulation(this);
		init();
	}

	/**
	 * Reinitialises the simulation. This is generally only for internal use.
	 */
	public void init() {
		neurons.init();
		synapses.init();
		reset();

		// Set aparapi execution mode based on network size (assume more synapses than neurons).
		Kernel.EXECUTION_MODE mode = Kernel.EXECUTION_MODE.GPU;
		if (synapses.getSize() < 512) {
			// Don't use JTP, for sizes less than 512 it is slower than SEQ.
			mode = Kernel.EXECUTION_MODE.SEQ;
			System.out.println("Using SEQ mode for small network.");
		}

		neurons.setExecutionMode(mode);
		synapses.setExecutionMode(mode);

		// If trying for an OpenCL mode, do a test step to see what execution mode actually gets used.
		// We can't allow some components to execute in GPU (or CPU?) mode and others not, otherwise buffer sharing used
		// when on GPU (CPU?) will not work. (and copying buffers/arrays back and forth every step would likely be too slow,
		// TODO: but perhaps in the future we could allow for this).
		if (Utility.executionModeIsOpenCL(mode)) {
			step();
			// If the execution modes that got used aren't the same, make sure they're compatible.
			if (neurons.getExecutionMode() != synapses.getExecutionMode()) {
				if (!Utility.executionModeIsOpenCL(neurons.getExecutionMode())) {
					System.err.println("Warning: could not run NeuronCollection in OpenCL mode.");
				}
				if (!Utility.executionModeIsOpenCL(synapses.getExecutionMode())) {
					System.err.println("Warning: could not run SynapseCollection in OpenCL mode.");
				}

				// Determine lowest execution mode of those that were used, and set all components to use it...
				mode = Utility.getLowestExecutionMode(neurons.getExecutionMode(), synapses.getExecutionMode());

				// ... with the exception of avoiding JTP for component collections of
				// size < 512 as it's less performant than SEQ.
				// (We already checked to see if there were less than 512 synapses above.)
				if (mode == Kernel.EXECUTION_MODE.JTP && neurons.getSize() < 512) {
					neurons.setExecutionMode(Kernel.EXECUTION_MODE.SEQ);
				} else {
					neurons.setExecutionMode(mode);
				}
				synapses.setExecutionMode(mode);
			}

			reset();
		}
	}

	/**
	 * Reset the simulation.
	 */
	public synchronized void reset() {
		neurons.reset();
		synapses.reset();
		step = 0;
	}

	/**
	 * Set the time resolution of the simulation. This will reinitialise and reset the  and 
	 * collections.
	 */
	public synchronized void setTimeResolution(int timeResolution) {
		this.timeResolution = timeResolution;
		init();
	}

	/**
	 * Get the time resolution of the simulation.
	 */
	public int getTimeResolution() {
		return timeResolution;
	}

	/**
	 * Get the duration in seconds of each simulation step.
	 */
	public double getStepPeriod() {
		return stepPeriod;
	}

	/**
	 * Simulate one time step.
	 */
	public synchronized void step() {
		neurons.step();
		synapses.step();
		step++;
	}

	/**
	 * Run the simulation for the given number of steps. This should be used in preference to using a loop of a fixed number of
	 * iterations that only calls step() as it allows for more efficient use of OpenCL execution by avoiding unnecessary buffer
	 * transfers between each step.
	 */
	public synchronized void run(int steps) {
		for (int s = 0; s < steps; s++) {
			neurons.step();
			synapses.step();
			step++;
		}
	}

	/**
	 * Returns the current simulation step.
	 */
	public synchronized long getStep() {
		return step;
	}

	/**
	 * Returns the current simulation time in seconds.
	 */
	public synchronized double getTime() {
		return step * stepPeriod;
	}

	/**
	 * Returns the neurons in this simulation.
	 * 
	 * @return The NeuronCollection belonging to this simulation.
	 */
	public NeuronCollection<? extends ComponentConfiguration> getNeurons() {
		return neurons;
	}

	/**
	 * Returns the synapses in this simulation.
	 * 
	 * @return The SynapseCollection belonging to this simulation.
	 */
	public SynapseCollection<? extends ComponentConfiguration> getSynapses() {
		return synapses;
	}

	public static void setSingleton(Simulation sim) {
		singleton = sim;
	}

	public static Simulation getSingleton() {
		return singleton;
	}

	public static void main(String[] args) {
		/*
		 * int timeResolution = 1000; //1 simulation step every 1ms. int simDuration = 1; //1 second int simSteps = simDuration
		 * * timeResolution;
		 * 
		 * NeuronCollectionFixedFrequency neurons = new NeuronCollectionFixedFrequency(2); neurons.addConfiguration(new
		 * NeuronConfigurationFixedFrequency(0.09)); neurons.addConfiguration(new NeuronConfigurationFixedFrequency(0.1));
		 * neurons.setComponentConfiguration(0, 0); neurons.setComponentConfiguration(0, 1);
		 * 
		 * SynapseCollectionPfister2006 synapses = new SynapseCollectionPfister2006(1);
		 * synapses.addConfiguration((SynapseConfigurationPfister2006) synapses.getConfigSingleton().getPreset(0));
		 * synapses.setComponentConfiguration(0, 0); synapses.setPreNeuron(0, 0); synapses.setPostNeuron(0, 1);
		 * 
		 * Simulation sim = new Simulation(1000, neurons, synapses);
		 * 
		 * long start = System.currentTimeMillis();
		 * 
		 * TestResults results = SynapseTest.singleTest(sim, simSteps, true);
		 * 
		 * long finish = System.currentTimeMillis(); System.out.println("Took " + ((finish - start) / 1000f) + " seconds.");
		 * 
		 * //SynapseTest.createChart(results, timeResolution, true, true);
		 */
		// setExecutionMode(Kernel.EXECUTION_MODE.JTP);
		for (int networkSize = 2; networkSize <= (2 << 20); networkSize *= 2) {
			double simDuration = 10;
			int timeResolution = 1000; // 1 simulation step every 1ms.
			int neuronConfigurationCount = 10;
			int simSteps = (int) Math.round(simDuration * timeResolution);

			FixedFrequencyNeuronCollection neurons = new FixedFrequencyNeuronCollection(networkSize);
			for (int i = 0; i < neuronConfigurationCount; i++) {
				neurons.addConfiguration(new FixedFrequencyNeuronConfiguration(i * 0.01 + 0.01));
			}

			for (int i = 0; i < networkSize; i++) {
				neurons.setComponentConfiguration(i, i % neuronConfigurationCount);
			}

			Pfister2006SynapseCollection synapses = new Pfister2006SynapseCollection(0);

			Simulation sim = new Simulation(timeResolution, neurons, synapses);

			double[] dummy = new double[2];

			float[] times = new float[2];

			for (int i = 0; i < 2; i++) {

				if (i == 1) {
					neurons.setExecutionMode(Kernel.EXECUTION_MODE.JTP);
				}
				// Run for 1 second.
				long start = System.currentTimeMillis();

				sim.run(simSteps);

				long finish = System.currentTimeMillis();

				// Read values to make sure the optimiser doesn't optimise out the simulation code.
				for (int o = 0; o < neurons.getSize(); o++) {
					dummy[i] += 1.0 / (neurons.getOutput(o) + 1);
				}
				times[i] = (finish - start) / 1000f;
			}
			System.out.println(networkSize + ": GPU: " + times[0] + ", CPU: " + times[1] + "  " + Arrays.toString(dummy));
		}
	}
}