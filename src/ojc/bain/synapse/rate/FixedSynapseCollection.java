package ojc.bain.synapse.rate;

import java.util.Arrays;

import ojc.bain.base.ComponentCollection;
import ojc.bain.base.ComponentConfiguration;
import ojc.bain.base.SynapseCollection;
import ojc.bain.synapse.spiking.Pfister2006SynapseConfiguration;

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
