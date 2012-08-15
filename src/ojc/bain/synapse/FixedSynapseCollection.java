package ojc.bain.synapse;

import java.util.Arrays;

import ojc.bain.base.ComponentCollection;
import ojc.bain.base.ComponentConfiguration;
import ojc.bain.base.SynapseCollection;

/**
 * Implements synapses with a fixed efficacy (weight).
 * 
 * @see Pfister2006SynapseConfiguration
 * 
 * @author Oliver J. Coleman
 */
public class FixedSynapseCollection extends SynapseCollection {
	public FixedSynapseCollection(int size) {
		this.size = size;
	}

	/**
	 * Returns a reference to the internal efficacy array, to facilitate setting the fixed efficacy values.
	 */
	public double[] getEfficacies() {
		return efficacy;
	}

	@Override
	public void init() {
		super.init();
		stateVariablesStale = false;
	}

	public void reset() {
		// Don't call super.reset() as it will reset all values in efficacy.
		Arrays.fill(synapseOutputs, 0);
		put(efficacy);
		put(synapseOutputs);
	}

	@Override
	public void run() {
		int synapseID = this.getGlobalId();
		if (synapseID >= size)
			return;
		super.run();
	}

	@Override
	public String[] getStateVariableNames() {
		String[] names = { "efficacy" };
		return names;
	}

	@Override
	public double[] getStateVariableValues(int synapseIndex) {
		ensureStateVariablesAreFresh();
		double[] values = { efficacy[synapseIndex] };
		return values;
	}

	@Override
	public void ensureStateVariablesAreFresh() {
	}

	@Override
	public ComponentConfiguration getConfigSingleton() {
		return null;
	}

	@Override
	public ComponentCollection createCollection(int size) {
		return new FixedSynapseCollection(size);
	}
}
