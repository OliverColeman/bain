package com.ojcoleman.bain;

import java.util.Arrays;
import java.util.Iterator;

import com.amd.aparapi.Kernel;
import com.ojcoleman.bain.base.*;
import com.ojcoleman.bain.neuron.spiking.FixedFrequencyNeuronCollection;
import com.ojcoleman.bain.neuron.spiking.FixedFrequencyNeuronConfiguration;
import com.ojcoleman.bain.synapse.spiking.Pfister2006SynapseCollection;

/**
 * <p>
 * The main class for running a neural network simulation. A NeuralNetwork is run at a specified time resolution, which
 * is the number of discrete simulation steps performed for each second of simulation time. A typical resolution is
 * 1000, or 1ms duration for each step.
 * </p>
 * 
 * <p>
 * A NeuralNetwork consists of a {@link com.ojcoleman.bain.base.NeuronCollection} and a
 * {@link com.ojcoleman.bain.base.SynapseCollection}. These collections of neural network components are designed to be
 * executable on SIMD hardware (eg a GPU) via OpenCL via Aparapi (See
 * {@link com.ojcoleman.bain.base.ComponentCollection} for more details).
 * </p>
 * 
 * <p>
 * To improve performance, ComponentCollection and extensions thereof use the explicit memory management feature of
 * Aparapi. When using SIMD hardware such as a GPU, the data to be processed must be transferred to the device, and the
 * results transferred back to the CPU after the computations have been performed. These transfers take a significant
 * amount of time. So data is only transferred when necessary (eg if it will be used by another kernel, or if it has
 * been requested).
 * </p>
 * <p>
 * If a preferred execution mode is not set (see
 * {@link #setPreferredExecutionMode(Kernel.EXECUTION_MODE preferredExecutionMode)} ), then for neuron or synapse
 * collections consisting of fewer than {@link #minimumSizeForGPU} neurons or synapses, use of a CPU-based execution
 * mode is forced as this is typically more performant than use of SIMD hardware; if using a CPU-based execution mode
 * then if the size is greater than or equal to {@link #minimumSizeForJTP} the JTP execution mode is used, otherwise the
 * SEQ execution mode is used.
 * </p>
 * 
 * @author Oliver J. Coleman
 */
public class NeuralNetwork {
	/**
	 * When automatically selecting an execution mode, this is the minimum number of components in a collection before
	 * the GPU execution mode is attempted. Default is 8192.
	 */
	protected int minimumSizeForGPU = 8192;

	/**
	 * Get the minimum number of components in a collection before the GPU execution mode is attempted, if using
	 * automatic mode selection.
	 */
	public int getMinimumSizeForGPU() {
		return minimumSizeForGPU;
	}

	/**
	 * Set the minimum number of components in a collection before the GPU execution mode is attempted, if using
	 * automatic mode selection.
	 */
	public void setMinimumSizeForGPU(int minimumSizeForGPU) {
		if (this.minimumSizeForGPU != minimumSizeForGPU) {
			this.minimumSizeForGPU = minimumSizeForGPU;
			selectExecutionModes();
		}
	}

	/**
	 * When automatically selecting an execution mode, this is the minimum number of components in a collection before
	 * the JTP execution mode is used. Default is Integer.MAX_VALUE (currently disabled, JTP is sloooow, for various
	 * reasons).
	 */
	protected int minimumSizeForJTP = Integer.MAX_VALUE; // 1048576;

	/**
	 * Get the minimum number of components in a collection before the GPU execution mode is attempted, if using
	 * automatic mode selection.
	 */
	public int getMinimumSizeForJTP() {
		return minimumSizeForJTP;
	}

	/**
	 * Set the minimum number of components in a collection before the JTP execution mode is attempted, if using
	 * automatic mode selection.
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
	 * Set the preferred execution mode, which will override an automatically selected mode if not null. Set to null to
	 * enable automatic mode selection.
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
	public NeuralNetwork(int timeResolution) {
		step = 0;
		this.timeResolution = timeResolution;
		stepPeriod = 1.0 / timeResolution;
	}

	/**
	 * Create a new neural network. Sets the time resolution of the given neuron and synapse collections to match that
	 * of this neural network and resets them.
	 * 
	 * @param timeResolution The number of discrete simulation steps performed for each second of simulation time. A
	 *            typical resolution is 1000, or 1ms duration for each step.
	 * @param neurons The NeuronCollection to use in the network.
	 * @param synapses TheSynapseCollection to use in the network.
	 */
	public NeuralNetwork(int timeResolution, NeuronCollection<? extends ComponentConfiguration> neurons, SynapseCollection<? extends ComponentConfiguration> synapses) {
		this.timeResolution = timeResolution;
		stepPeriod = 1.0 / timeResolution;
		this.neurons = neurons;
		this.synapses = synapses;
		neurons.setNetwork(this);
		synapses.setNetwork(this);
		selectExecutionModes();
		init();
	}

	/**
	 * Create a new neural network. Sets the time resolution of the given neuron and synapse collections to match that
	 * of this network and resets them. Allows specifying the preferred execution mode, which will override the
	 * automatic selection of a mode based on network size.
	 * 
	 * @param timeResolution The number of discrete simulation steps performed for each second of simulation time. A
	 *            typical resolution is 1000, or 1ms duration for each step.
	 * @param neurons The NeuronCollection to use in the network.
	 * @param synapses TheSynapseCollection to use in the network.
	 * @param preferredExecutionMode The preferred execution mode, which will override the automatic selection of a mode
	 *            based on network size.
	 */
	public NeuralNetwork(int timeResolution, NeuronCollection<? extends ComponentConfiguration> neurons, SynapseCollection<? extends ComponentConfiguration> synapses, Kernel.EXECUTION_MODE preferredExecutionMode) {
		this.timeResolution = timeResolution;
		stepPeriod = 1.0 / timeResolution;
		this.neurons = neurons;
		this.synapses = synapses;
		this.preferredExecutionMode = preferredExecutionMode;
		neurons.setNetwork(this);
		synapses.setNetwork(this);
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
		// when on GPU (CPU?) will not work. (and copying buffers/arrays back and forth every step would likely be too
		// slow,
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
		stepPeriod = 1.0 / timeResolution;
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
		// We step synapses first in case the neuron outputs have been modified, for example to provide external input
		// to the network.
		synapses.step();
		neurons.step();
		step++;
	}

	/**
	 * Run the simulation for the given number of steps. This should be used in preference to using a loop of a fixed
	 * number of iterations that only calls step() as it allows for more efficient use of OpenCL execution by avoiding
	 * unnecessary buffer transfers between each step.
	 */
	public synchronized void run(int steps) {
		for (int s = 0; s < steps; s++) {
			// We step synapses first in case the neuron outputs have been modified, for example to provide external
			// input to the network.
			if (debug) System.out.println("Synapses:");
			synapses.step();
			if (debug) System.out.println("Neurons:");
			neurons.step();
			step++;
		}
	}

	/**
	 * Returns the current simulation step number.
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
	 * Returns the neurons in this network.
	 * 
	 * @return The NeuronCollection belonging to this network.
	 */
	public NeuronCollection<? extends ComponentConfiguration> getNeurons() {
		return neurons;
	}

	/**
	 * Sets the neurons in this network. This will reinitialise and reset the simulation.
	 */
	public void setNeurons(NeuronCollection<? extends ComponentConfiguration> neurons) {
		if (this.neurons != null) {
			this.neurons.setNetwork(null);
		}
		this.neurons = neurons;
		neurons.setNetwork(this);
		selectExecutionModes();
		init();
		reset();
	}

	/**
	 * Returns the synapses in this network.
	 * 
	 * @return The SynapseCollection belonging to this network.
	 */
	public SynapseCollection<? extends ComponentConfiguration> getSynapses() {
		return synapses;
	}

	/**
	 * Sets the synapses in this network. This will reinitialise and reset the simulation.
	 */
	public void setSynapses(SynapseCollection<? extends ComponentConfiguration> synapses) {
		if (this.synapses != null) {
			this.synapses.setNetwork(null);
		}
		this.synapses = synapses;
		synapses.setNetwork(this);
		selectExecutionModes();
		init();
		reset();
	}

	/**
	 * Release any resources associated with this NeuralNetwork. It's important to call this.
	 */
	public void dispose() {
		if (neurons != null)
			neurons.dispose();
		if (synapses != null)
			synapses.dispose();
	}

	private boolean debug;
	public boolean debug() {
		return debug;
	}
	public void setDebug(boolean d) {
		debug = d;
	}
}
