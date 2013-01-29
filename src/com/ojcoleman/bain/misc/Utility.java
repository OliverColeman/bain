package com.ojcoleman.bain.misc;

import com.amd.aparapi.Kernel;

/**
 * Miscellaneous collection of functions and utilities.
 * 
 * @author Oliver J. Coleman
 */
public class Utility {
	/**
	 * Calculate <em>floor(lg(x))</em> (where <em>lg()</em> is the base-2 logarithm of <em>x</em>). Returns nonsense results if <em>x &lt;= 0</em>.
	 */
	public static int log2int(int x) {
		return 31 - Integer.numberOfLeadingZeros(x);
	}

	static final Kernel.EXECUTION_MODE[] modeOrder = new Kernel.EXECUTION_MODE[] { Kernel.EXECUTION_MODE.SEQ, Kernel.EXECUTION_MODE.JTP, Kernel.EXECUTION_MODE.CPU, Kernel.EXECUTION_MODE.GPU };

	/**
	 * Determine the "lowest" execution mode between two given modes. The execution modes from lowest to highest are: SEQ, JTP, CPU, GPU.
	 */
	public static Kernel.EXECUTION_MODE getLowestExecutionMode(Kernel.EXECUTION_MODE a, Kernel.EXECUTION_MODE b) {
		for (Kernel.EXECUTION_MODE mode : modeOrder) {
			if (a == mode || b == mode) {
				return mode;
			}
		}
		return null;
	}

	/**
	 * Returns true iff the given execution mode makes use of OpenCL. These execution modes are GPU and CPU.
	 */
	public static boolean executionModeIsOpenCL(Kernel.EXECUTION_MODE mode) {
		return mode == Kernel.EXECUTION_MODE.GPU || mode == Kernel.EXECUTION_MODE.CPU;
	}
}
