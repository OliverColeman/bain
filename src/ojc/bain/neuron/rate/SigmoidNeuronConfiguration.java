package ojc.bain.neuron.rate;

import ojc.bain.base.ComponentConfiguration;
import ojc.bain.base.NeuronConfiguration;

/**
 * Configuration for {@link SigmoidNeuronCollection}.
 * 
 * @author Oliver J. Coleman
 */
public class SigmoidNeuronConfiguration extends NeuronConfiguration {
	/**
	 * Slope of Sigmoid function. Default is 4.924273.
	 */
	public double slope = 4.924273;

	/**
	 * Create a SigmoidNeuronConfiguration. This is used for retrieving a 
	 * configuration singleton or a default configuration.
	 */
	public SigmoidNeuronConfiguration() {
	}

	/**
	 * Create a SigmoidNeuronConfiguration.
	 * 
	 * @param slope The slope of the Sigmoid function.
	 */
	public SigmoidNeuronConfiguration(double slope) {
		this.slope = slope;
	}

	@Override
	public String[] getParameterNames() {
		return new String[] { "slope" };
	}

	@Override
	public String[] getPresetNames() {
		return new String[] {};
	}

	@Override
	public ComponentConfiguration getPreset(int index) {
		return null;
	}

	@Override
	public ComponentConfiguration createConfiguration() {
		return new SigmoidNeuronConfiguration();
	}

}