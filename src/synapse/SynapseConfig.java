package synapse;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * A base class for all Synapse configurations. Provides methods to query the configuration for available parameters, 
 * get and set those parameters, and work with preset configurations. 
 * Typically sub-classes will declare a set of instance variables that can be accessed directly by the corresponding 
 * Synapse class for efficiency reasons (rather than using the generic getParameterNames() and getParameterValues() 
 * methods).
 * Sub-classes of Synapse implement the method getConfigSingleton() to provide a reference to the implementation- 
 * specific sub-class of this class.  
 * 
 * @author Oliver J. Coleman
 */
public abstract class SynapseConfig {
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
	}
	
	public abstract String[] getPresetNames();
	
	public abstract SynapseConfig getPreset(int index);
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof SynapseConfig) {
			return Arrays.equals(this.getParameterValues(), ((SynapseConfig) other).getParameterValues());
		}
		return false;
	}
}