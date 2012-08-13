package ojc.bain.test;

import java.util.Arrays;

import com.amd.aparapi.Kernel;

public class Test1 {
	TestKernel kernel;

	public Test1(boolean explicit) {
		kernel = new TestKernel();
		kernel.setExplicit(explicit);
	}

	public void step() {
		kernel.step();
	}

	private class TestKernel extends Kernel {
		int[] simStep = new int[1];
		int[] neuronOutputs = new int[3];

		public void step() {
			int simSteps = 16;
			int[][] log = new int[neuronOutputs.length][simSteps];
			put(neuronOutputs);
			for (simStep[0] = 0; simStep[0] < simSteps; simStep[0]++) {
				put(simStep).execute(neuronOutputs.length).get(neuronOutputs);
				for (int n = 0; n < neuronOutputs.length; n++)
					log[n][simStep[0]] = neuronOutputs[n];
			}
			System.out.println(kernel.getExecutionMode() + (isExplicit() ? ", explicit" : ", auto"));
			for (int n = 0; n < neuronOutputs.length; n++)
				System.out.println(Arrays.toString(log[n]));
		}

		@Override
		public void run() {
			int neuronID = getGlobalId();
			neuronOutputs[neuronID] = neuronID + neuronOutputs[neuronID] + simStep[0];
		}
	}

	public static void main(String[] args) {
		Test1 t1 = new Test1(false);
		t1.step();
		System.out.println();
		Test1 t2 = new Test1(true);
		t2.step();
	}
}