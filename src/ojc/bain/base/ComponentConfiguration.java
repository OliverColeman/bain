package ojc.bain.base;

import java.util.*;

/**
 * A base class for all configuration objects for neural network elements (e.g. Neurons and Synapses). Provides methods to query
 * the configuration for available parameters, get and set those parameters, and work with preset configurations.
 * 
 * Typically sub-classes will declare a set of instance variables that can be accessed directly by the corresponding components
 * class for efficiency reasons (rather than using the generic getParameterNames() and getParameterValues() methods). If
 * parameters are set directly the fireChangeEvent() method should be invoked after setting the parameter(s).
 * 
 * Sub-classes of a components base class should implement the method getConfigSingleton() to provide a reference to the
 * implementation- specific sub-class of this class.
 * 
 * @author Oliver J. Coleman
 */
public abstract class ComponentConfiguration {
	Vector<ComponentConfigurationListener> listeners = new Vector<ComponentConfigurationListener>();

	/**
	 * Sub-classes should override this method to provide a list of parameter names.
	 */
	public abstract String[] getParameterNames();

	/**
	 * Sub-classes can override this method to provide the current parameter values in the same order as that given by
	 * getParameterLabels(). The default implementation uses getParameterNames() and Java's reflection abilities to get the
	 * parameter values from the declared public instance variables (and assumes that the labels given by getParameterNames()
	 * are the same as the declared variable names).
	 */
	public double[] getParameterValues() {
		String[] parameterLabels = getParameterNames();
		double[] params = new double[parameterLabels.length];
		try {
			for (int pi = 0; pi < parameterLabels.length; pi++) {
				params[pi] = this.getClass().getField(parameterLabels[pi]).getDouble(this);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} // Just ignore exceptions thrown by getField rather than force calling
			// code to deal with handling exceptions.
		return params;
	}

	/**
	 * Sub-classes can override this method to provide the specified parameter value. The default implementation uses Java's
	 * reflection abilities to get the parameter value from the declared public instance variables, assuming that the parameter
	 * name given is the same as the declared variable name.
	 * 
	 * @param param The name of parameter;
	 */
	public double getParameterValue(String param) {
		try {
			return this.getClass().getField(param).getDouble(this);
		} catch (Exception e) {
		} // Just ignore exceptions thrown by getField rather than force calling
			// code to deal with handling exceptions.
		return 0;
	}

	/**
	 * Sub-classes can override this method to allow setting the parameter values. The default implementation uses
	 * getParameterNames() and Java's reflection abilities to set the parameter values on the declared public instance variables
	 * (and assumes that the labels given by getParameterNames() are the same as the declared variable names).
	 * 
	 * @param params The new parameter values, in the same order as that given by getParameterNames().
	 */
	public void setParameterValues(double[] params) {
		String[] parameterLabels = getParameterNames();
		boolean changedAny = false;
		try {
			for (int pi = 0; pi < parameterLabels.length; pi++) {
				this.getClass().getField(parameterLabels[pi]).setDouble(this, params[pi]);
				changedAny = true;
			}
		} catch (Exception e) {
		} // Just ignore exceptions thrown by getField rather than force calling
			// code to deal with handling exceptions.

		if (changedAny) {
			fireChangeEvent();
		}
	}

	/**
	 * Sub-classes can override this method to allow setting a parameter value. The default implementation uses Java's
	 * reflection abilities to set the parameter value on the declared public instance variables, assuming that the parameter
	 * name given is the same as the declared variable name.
	 * 
	 * @param param The name of parameter;
	 * @param value The new value.
	 */
	public void setParameterValue(String param, double value) {
		try {
			this.getClass().getField(param).setDouble(this, value);
			fireChangeEvent();
		} catch (Exception e) {
		} // Just ignore exceptions thrown by getField rather than force calling
			// code to deal with handling exceptions.
	}

	public abstract String[] getPresetNames();

	public abstract ComponentConfiguration getPreset(int index);

	@Override
	/**
	 * ComponentConfiguration objects are considered equal if they are of the same sub-class and 
	 * have the same parameter values for the parameters included in getParameterNames().
	 */
	public boolean equals(Object other) {
		if (other instanceof ComponentConfiguration) {
			return Arrays.equals(this.getParameterValues(), ((ComponentConfiguration) other).getParameterValues());
		}
		return false;
	}

	public void addListener(ComponentConfigurationListener l) {
		listeners.add(l);
	}

	public void removeListener(ComponentConfigurationListener l) {
		listeners.remove(l);
	}

	public void removeAllListeners() {
		listeners.removeAllElements();
	}

	/**
	 * Notify all ComponentConfigurationListener that parameter values have changed. This method should be invoked manually if
	 * parameters are set directly (and not via the setParameterValue() method).
	 */
	public void fireChangeEvent() {
		for (ComponentConfigurationListener l : listeners) {
			l.configurationChanged(this);
		}
	}

	/**
	 * Factory method to create a new configuration (typically invoked on a singleton retrieved from
	 * myConfigurableComponentCollection.getConfigSingleton()).
	 */
	public abstract ComponentConfiguration createConfiguration();
}