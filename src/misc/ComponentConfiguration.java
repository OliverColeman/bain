package misc;

import java.util.Arrays;

/**
 * A base class for all configuration objects for neural network elements (e.g. Neurons and Synapses). 
 * Provides methods to query the configuration for available parameters, get and set those parameters, and work with preset configurations. 
 * Typically sub-classes will declare a set of instance variables that can be accessed directly by the corresponding 
 * components class for efficiency reasons (rather than using the generic getParameterNames() and getParameterValues() methods).
 * Sub-classes of a components base class typically implement the method getConfigSingleton() to provide a reference to the implementation- 
 * specific sub-class of this class.
 * 
 * @author Oliver J. Coleman
 */
public abstract class ComponentConfiguration {
	protected int timeResolution = 1000;
	protected double stepPeriod = 1.0 / timeResolution;
	
	/**
	 * Get the current time resolution. This is the number of simulation steps per second.
	 * Default is 1000, corresponding to a simulation step size of 1 millisecond.
	 */ 
	public int getTimeResolution() {
		return timeResolution;
	}
	/**
	 * Set the current time resolution. This is the number of simulation steps per second.
	 * Default is 1000, corresponding to a simulation step size of 1 millisecond.
	 */ 
	public void setTimeResolution(int timeResolution) {
		this.timeResolution = timeResolution;
		init();
	}
	
	/**
	 * Returns the duration of each simulation step in seconds, based on the time resolution.
	 */
	public double getStepPeriod() {
		return stepPeriod;
	}


	/**
	 * Initialise values generated from the parameters. 
	 * This method should be called if any parameters are changed directly (rather than via the setParameterValue method).
	 * Sub-classes should override this method to update the value of any variables they declare which depend on the values of other parameters.
	 * This super-class method should be called from an overriding method.
	 */
	public void init() {
		stepPeriod = 1.0 / timeResolution;
	}
	
	
	/**
	 * Sub-classes should override this method to provide a list of parameter names.
	 */
	public abstract String[] getParameterNames();
	
	/**
	 * Sub-classes can override this method to provide the current parameter values in the same order as that given by getParameterLabels().
	 * The default implementation uses getParameterNames() and Java's reflection abilities to get the parameter values from the declared public 
	 * instance variables (and assumes that the labels given by getParameterNames() are the same as the declared variable names). 
	 */
	public double[] getParameterValues() {
		String[] parameterLabels = getParameterNames();
		double[] params = new double[parameterLabels.length];
		try {
			for (int pi = 0; pi < parameterLabels.length; pi++) {
				params[pi] = this.getClass().getField(parameterLabels[pi]).getDouble(this);
			}
		}
		catch (Exception e) {} // Just ignore exceptions thrown by getField rather than force calling code to deal with handling exceptions. 
		return params;
	}
	
	/**
	 * Sub-classes can override this method to provide the specified parameter value.
	 * The default implementation uses Java's reflection abilities to get the parameter value from the declared public 
	 * instance variables, assuming that the parameter name given is the same as the declared variable name.
	 * @param param The name of parameter;
	 */
	public double getParameterValue(String param) {
		try {
			return this.getClass().getField(param).getDouble(this);
		}
		catch (Exception e) {} // Just ignore exceptions thrown by getField rather than force calling code to deal with handling exceptions. 
		return 0;
	}
	
	/**
	 * Sub-classes can override this method to allow setting the parameter values.
	 * The default implementation uses getParameterNames() and Java's reflection abilities to set the parameter values on the declared public 
	 * instance variables (and assumes that the labels given by getParameterNames() are the same as the declared variable names).
	 * @param params The new parameter values, in the same order as that given by getParameterNames().
	 */
	public void setParameterValues(double[] params) {
		String[] parameterLabels = getParameterNames();
		try {
			for (int pi = 0; pi < parameterLabels.length; pi++) {
				this.getClass().getField(parameterLabels[pi]).setDouble(this, params[pi]);
			}
		}
		catch (Exception e) {} // Just ignore exceptions thrown by getField rather than force calling code to deal with handling exceptions.
		init();
	}
	
	/**
	 * Sub-classes can override this method to allow setting a parameter value.
	 * The default implementation uses Java's reflection abilities to set the parameter value on the declared public 
	 * instance variables, assuming that the parameter name given is the same as the declared variable name.
	 * @param param The name of parameter;
	 * @param value The new value.
	 */
	public void setParameterValue(String param, double value) {
		try {
			this.getClass().getField(param).setDouble(this, value);
		}
		catch (Exception e) {} // Just ignore exceptions thrown by getField rather than force calling code to deal with handling exceptions.
		init();
	}
	
	public abstract String[] getPresetNames();
	
	public abstract ComponentConfiguration getPreset(int index);
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof ComponentConfiguration) {
			return Arrays.equals(this.getParameterValues(), ((ComponentConfiguration) other).getParameterValues());
		}
		return false;
	}
}