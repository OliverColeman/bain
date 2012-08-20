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
 * The main class for running a neural network simulation. A Simulation is run at a specified time resolution, which is the number of discrete simulation steps
 * performed for each second of simulation time. A typical resolution is 1000, or 1ms duration for each step.
 * </p>
 * 
 * <p>
 * A Simulation consists of a {@link ojc.bain.base.NeuronCollection} and a {@link ojc.bain.base.SynapseCollection}. These collections of neural network
 * components are designed to be executable on SIMD hardware (eg a GPU) via OpenCL via Aparapi (See {@link ojc.bain.base.ComponentCollection} for more details).
 * </p>
 * 
 * <p>
 * To improve performance, ComponentCollection and extensions thereof use the explicit memory management feature of Aparapi. When using SIMD hardware such as a
 * GPU, the data to be processed must be transferred to the device, and the results transferred back to the CPU after the computations have been performed.
 * These transfers take a significant amount of time. So data is only transferred when necessary (eg if it will be used by another kernel, or if it has been
 * requested).
 * </p>
 * <p>
 * If a preferred execution mode is not set (see {@link #setPreferredExecutionMode(Kernel.EXECUTION_MODE preferredExecutionMode)} ), then for neuron or synapse
 * collections consisting of fewer than {@link #minimumSizeForGPU} neurons or synapses, the Simulation forces the use of a CPU-based execution mode as this is
 * typically more performant than use of SIMD hardware; if using a CPU-based execution mode then if the size is greater than or equal to
 * {@link #minimumSizeForJTP} the JTP execution mode is used, otherwise the SEQ execution mode is used.
 * </p>
 * 
 * @author Oliver J. Coleman
 */
public class Simulation {
	static Simulation singleton = new Simulation(1000);
	/**
	 * When automatically selecting an execution mode, this is the minimum number of components in a collection before the GPU execution mode is attempted.
	 * Default is 8192.
	 */
	protected int minimumSizeForGPU = 8192;

	/**
	 * Get the minimum number of components in a collection before the GPU execution mode is attempted, if using automatic mode selection.
	 */
	public int getMinimumSizeForGPU() {
		return minimumSizeForGPU;
	}

	/**
	 * Set the minimum number of components in a collection before the GPU execution mode is attempted, if using automatic mode selection.
	 */
	public void setMinimumSizeForGPU(int minimumSizeForGPU) {
		if (this.minimumSizeForGPU != minimumSizeForGPU) {
			this.minimumSizeForGPU = minimumSizeForGPU;
			selectExecutionModes();
		}
	}

	/**
	 * When automatically selecting an execution mode, this is the minimum number of components in a collection before the JTP execution mode is used. Default
	 * is Integer.MAX_VALUE (currently disabled, JTP is sloooow, for various reasons).
	 */
	protected int minimumSizeForJTP = Integer.MAX_VALUE; // 1048576;

	/**
	 * Get the minimum number of components in a collection before the GPU execution mode is attempted, if using automatic mode selection.
	 */
	public int getMinimumSizeForJTP() {
		return minimumSizeForJTP;
	}

	/**
	 * Set the minimum number of components in a collection before the JTP execution mode is attempted, if using automatic mode selection.
	 */
	public void setMinimumSizeForJTP(int minimumSizeForJTP) {
		if (this.minimumSizeForJTP != minimumSizeForJTP) {
			this.minimumSizeForJTP = minimumSizeForJTP;
			selectExecutionModes();
		}
	}

	/**
	 * The preferred execution mode, which will override an automatically selected mode if not null.
	 */
	protected Kernel.EXECUTION_MODE preferredExecutionMode;

	/**
	 * Get the preferred execution mode, which will override an automatically selected mode if not null.
	 */
	public Kernel.EXECUTION_MODE getPreferredExecutionMode() {
		return preferredExecutionMode;
	}

	/**
	 * Set the preferred execution mode, which will override an automatically selected mode if not null. Set to null to enable automatic mode selection.
	 */
	public void setPreferredExecutionMode(Kernel.EXECUTION_MODE preferredExecutionMode) {
		if (this.preferredExecutionMode != preferredExecutionMode) {
			this.preferredExecutionMode = preferredExecutionMode;
			selectExecutionModes();
		}
	}

	protected long step;
	protected int timeResolution = 1000;
	protected double stepPeriod = 1.0 / timeResolution;

	protected NeuronCollection<? extends ComponentConfiguration> neurons;
	protected SynapseCollection<? extends ComponentConfiguration> synapses;

	/**
	 * Create a new simulation.
	 */
	public Simulation(int timeResolution) {
		step = 0;
		this.timeResolution = timeResolution;
	}

	/**
	 * Create a new simulation. Sets the time resolution of all given Neurons and Synapses to match that of this Simulation and resets them.
	 * 
	 * @param timeResolution The number of discrete simulation steps performed for each second of simulation time. A typical resolution is 1000, or 1ms duration
	 *            for each step.
	 * @param neurons The NeuronCollection to use in the simulation.
	 * @param synapses TheSynapseCollection to use in the simulation.
	 */
	public Simulation(int timeResolution, NeuronCollection<? extends ComponentConfiguration> neurons, SynapseCollection<? extends ComponentConfiguration> synapses) {
		this.timeResolution = timeResolution;
		this.neurons = neurons;
		this.synapses = synapses;
		neurons.setSimulation(this);
		synapses.setSimulation(this);
		selectExecutionModes();
		init();
	}

	/**
	 * Create a new simulation. Sets the time resolution of all given Neurons and Synapses to match that of this Simulation and resets them. Allows specifying
	 * the preferred execution mode, which will override the automatic selection of a mode based on network size.
	 * 
	 * @param timeResolution The number of discrete simulation steps performed for each second of simulation time. A typical resolution is 1000, or 1ms duration
	 *            for each step.
	 * @param neurons The NeuronCollection to use in the simulation.
	 * @param synapses TheSynapseCollection to use in the simulation.
	 * @param preferredExecutionMode The preferred execution mode, which will override the automatic selection of a mode based on network size.
	 */
	public Simulation(int timeResolution, NeuronCollection<? extends ComponentConfiguration> neurons, SynapseCollection<? extends ComponentConfiguration> synapses, Kernel.EXECUTION_MODE preferredExecutionMode) {
		this.timeResolution = timeResolution;
		this.neurons = neurons;
		this.synapses = synapses;
		this.preferredExecutionMode = preferredExecutionMode;
		neurons.setSimulation(this);
		synapses.setSimulation(this);
		selectExecutionModes();
		init();
	}

	/**
	 * Reinitialises the simulation. This is generally only for internal use.
	 */
	public void init() {
		neurons.init();
		synapses.init();
		reset();
	}

	protected void selectExecutionModes() {
		Kernel.EXECUTION_MODE mode = preferredExecutionMode;
		ComponentCollection[] collections = new ComponentCollection[] { neurons, synapses };
		// TODO below code doesn't work when minimumSizeForJTP > minimumSizeForGPU
		for (ComponentCollection c : collections) {
			if (preferredExecutionMode != null) {
				c.setExecutionMode(preferredExecutionMode);
			} else if (c.getSize() < minimumSizeForJTP) {
				c.setExecutionMode(Kernel.EXECUTION_MODE.SEQ);
			} else if (c.getSize() < minimumSizeForGPU) {
				c.setExecutionMode(Kernel.EXECUTION_MODE.JTP);
			} else {
				c.setExecutionMode(Kernel.EXECUTION_MODE.GPU);
			}
		}

		// SHARED BUFFERS NOT IMPLEMENTED IN APARAPI! See https://code.google.com/p/aparapi/issues/detail?id=56
		// If trying for an OpenCL mode, do a test step to see what execution mode actually gets used.
		// We can't allow some components to execute in GPU (or CPU?) mode and others not, otherwise buffer sharing used
		// when on GPU (CPU?) will not work. (and copying buffers/arrays back and forth every step would likely be too slow,
		// TODO: but perhaps in the future we could allow for this).
		// if (Utility.executionModeIsOpenCL(mode)) {
		// step();
		// // If the execution modes that got used aren't the same, make sure they're compatible.
		// if (neurons.getExecutionMode() != synapses.getExecutionMode()) {
		// if (!Utility.executionModeIsOpenCL(neurons.getExecutionMode())) {
		// System.err.println("Warning: could not run NeuronCollection in OpenCL mode.");
		// }
		// if (!Utility.executionModeIsOpenCL(synapses.getExecutionMode())) {
		// System.err.println("Warning: could not run SynapseCollection in OpenCL mode.");
		// }
		//
		// // Determine lowest execution mode of those that were used, and set all components to use it...
		// mode = Utility.getLowestExecutionMode(neurons.getExecutionMode(), synapses.getExecutionMode());
		//
		// // ... with the exception of avoiding JTP for component collections of
		// // size < 512 as it's less performant than SEQ.
		// // (We already checked to see if there were less than 512 synapses above.)
		// if (mode == Kernel.EXECUTION_MODE.JTP && neurons.getSize() < 512) {
		// neurons.setExecutionMode(Kernel.EXECUTION_MODE.SEQ);
		// } else {
		// neurons.setExecutionMode(mode);
		// }
		// synapses.setExecutionMode(mode);
		// }
		//
		// reset();
		// }
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
	 * Set the time resolution of the simulation. This will reinitialise and reset the and collections.
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
	 * Run the simulation for the given number of steps. This should be used in preference to using a loop of a fixed number of iterations that only calls
	 * step() as it allows for more efficient use of OpenCL execution by avoiding unnecessary buffer transfers between each step.
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
		 * int timeResolution = 1000; //1 simulation step every 1ms. int simDuration = 1; //1 second int simSteps = simDuration * timeResolution;
		 * 
		 * NeuronCollectionFixedFrequency neurons = new NeuronCollectionFixedFrequency(2); neurons.addConfiguration(new
		 * NeuronConfigurationFixedFrequency(0.09)); neurons.addConfiguration(new NeuronConfigurationFixedFrequency(0.1)); neurons.setComponentConfiguration(0,
		 * 0); neurons.setComponentConfiguration(0, 1);
		 * 
		 * SynapseCollectionPfister2006 synapses = new SynapseCollectionPfister2006(1); synapses.addConfiguration((SynapseConfigurationPfister2006)
		 * synapses.getConfigSingleton().getPreset(0)); synapses.setComponentConfiguration(0, 0); synapses.setPreNeuron(0, 0); synapses.setPostNeuron(0, 1);
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
