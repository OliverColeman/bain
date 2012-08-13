package ojc.bain.test;

import com.amd.aparapi.Kernel;
import com.amd.aparapi.Range;

public class Test2 extends Kernel {
	int size; // Number of work items.
	int iterations; // Number of times to execute kernel.
	int[] step = new int[1]; // Pass current iteration number to kernel.
	double[] outputs; // Output from each work item.

	public Test2(int size, int iterations) {
		this.size = size;
		this.iterations = iterations;
		outputs = new double[size];
		setExplicit(true);
	}

	public void go() {
		Range range1 = Range.create(size, 1);
		Range range4 = Range.create(size, 4);
		put(outputs);
		long start = System.currentTimeMillis();
		for (step[0] = 0; step[0] < iterations; step[0]++) {
			put(step);
			if (getExecutionMode() == Kernel.EXECUTION_MODE.SEQ) {
				execute(range1);
			} else if (getExecutionMode() == Kernel.EXECUTION_MODE.JTP) {
				// execute(range4);
				execute(size);
			} else {
				execute(size);
			}
		}
		long finish = System.currentTimeMillis();
		get(outputs);
		double dummy = 0;
		for (int n = 0; n < size; n++) {
			dummy += outputs[n];
		}
		System.out.print(getExecutionMode() + ": " + ((finish - start) / 1000f) + "s\t");
	}

	@Override
	public void run() {
		int id = getGlobalId();
		outputs[id] = Math.sin(id) + Math.cos(id) * Math.tan(id) + Math.acos(1.0 / (id + 1)) * Math.asin(1.0 / (id + 1)) + Math.atan(id) * Math.cbrt(id) + Math.cosh(id) * Math.exp(id);
	}

	public static void main(String[] args) {
		for (int size = 2; size <= (2 << 20); size *= 2) {
			System.out.print(size + ": \t");
			Test2 t = new Test2(size, 1024);
			t.setExecutionMode(Kernel.EXECUTION_MODE.SEQ);
			t.go();
			t.setExecutionMode(Kernel.EXECUTION_MODE.JTP);
			t.go();
			t.setExecutionMode(Kernel.EXECUTION_MODE.GPU);
			t.go();
			System.out.println();
		}
	}
}