package ojc.bain.test;

import com.amd.aparapi.Kernel;
import com.amd.aparapi.Range;

public class Test3 extends Kernel {
	int size; // Number of work items.
	int iterations; // Number of times to execute kernel.
	public double[] input, output;

	public Test3(int size) {
		this.size = size;
		input = new double[size];
		output = new double[size];
		setExplicit(true);
		put(output);
	}

	public void go() {
		put(input);
		execute(size);
		get(output);
	}

	@Override
	public void run() {
		int id = getGlobalId();
		output[id] = input[id];
		input[id] = 0;
	}

	public static void main(String[] args) {
		int size = 16;
		Test3 k1 = new Test3(size);
		Test3 k2 = new Test3(size);
		k2.input = k1.output;
		k1.input = k2.output;

		for (int i = 0; i < size; i++) {
			k1.input[i] = (int) (Math.random() * 10);
		}

		for (int s = 0; s < 2; s++) {
			if (size <= 32)
				printArray(k1.output);
			if (size <= 32)
				printArray(k2.output);

			k1.go();
			k2.go();
		}
		System.out.println(k1.getExecutionMode());
	}

	private static void printArray(double[] a) {
		for (int i = 0; i < a.length; i++) {
			System.out.print(a[i] + "\t");
		}
		System.out.println();
	}
}