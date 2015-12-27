package com.ojcoleman.bain.base;

import java.lang.reflect.Field;
import java.util.*;

/**
 * A base class for configuration objects for neural network elements (e.g. Neurons and Synapses). Provides methods to
 * query the configuration for available parameters, get and set those parameters, and work with preset configurations.
 * Configuration classes should extend {@link NeuronConfiguration} or {@link SynapseConfiguration}.
 * 
 * Typically sub-classes will declare a set of instance variables that can be accessed directly by the corresponding
 * components class for efficiency reasons (rather than using the generic getParameterNames() and getParameterValues()
 * methods). If parameters are set directly the fireChangeEvent() method should be invoked after setting the
 * parameter(s).
 * 
 * Sub-classes of a components base class should implement the method getConfigSingleton() to provide a reference to the
 * implementation- specific sub-class of this class.
 * 
 * @author Oliver J. Coleman
 */
public abstract class ComponentConfiguration {
	ArrayList<ComponentConfigurationListener> listeners = new ArrayList<ComponentConfigurationListener>();

	/**
	 * The name of the configuration. This may be a preset name or some other name.
	 */
	public String name;

	/**
	 * Sub-classes should override this method to provide a list of parameter names.
	 */
	public abstract String[] getParameterNames();

	/**
	 * Sub-classes can override this method to provide the current parameter values in the same order as that given by
	 * getParameterLabels(). The default implementation uses getParameterNames() and Java's reflection abilities to get
	 * the parameter values from the declared public instance variables (and assumes that the labels given by
	 * getParameterNames() are the same as the declared variable names).
	 */
	public double[] getParameterValues() {
		String[] parameterLabels = getParameterNames();
		double[] params = new double[parameterLabels.length];
		for (int pi = 0; pi < parameterLabels.length; pi++) {
			params[pi] = getParameterValue(parameterLabels[pi]);
		}
		return params;
	}

	/**
	 * Sub-classes can override this method to provide the specified parameter value. The default implementation uses
	 * Java's reflection abilities to get the parameter value from the declared public instance variables, assuming that
	 * the parameter name given is the same as the declared variable name. Note that all field types are converted to
	 * double using a direct cast, except in the case of a boolean field for which a value of false results in 0 and
	 * true in 1.
	 * 
	 * @param param The name of parameter;
	 */
	public double getParameterValue(String param) {
		try {
			Field f = this.getClass().getField(param);
			Class t = f.getType();
			if (t.equals(boolean.class) || t.equals(Boolean.class))
				return f.getBoolean(this) ? 1 : 0;
			else if (t.equals(byte.class) || t.equals(Byte.class))
				return f.getByte(this);
			else if (t.equals(char.class) || t.equals(Character.class))
				return f.getChar(this);
			else if (t.equals(short.class) || t.equals(Short.class))
				return f.getShort(this);
			else if (t.equals(int.class) || t.equals(Integer.class))
				return f.getInt(this);
			else if (t.equals(long.class) || t.equals(Long.class))
				return f.getLong(this);
			else if (t.equals(float.class) || t.equals(Float.class))
				return f.getFloat(this);
			else if (t.equals(double.class) || t.equals(Double.class))
				return f.getDouble(this);
			else {
				throw new IllegalArgumentException("Unsupported field type, only primitive types and their corresponding wrappers are supported.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} // Just ignore exceptions thrown by getField rather than force calling
			// code to deal with handling exceptions.
		return 0;
	}

	/**
	 * Sub-classes can override this method to allow setting the parameter values. The default implementation uses
	 * getParameterNames() and Java's reflection abilities to set the parameter values on the declared public instance
	 * variables (and assumes that the labels given by getParameterNames() are the same as the declared variable names).
	 * Note that if special handling is only required for some fields/parameters then
	 * {@link #setParameterValue(String, double, boolean)} can be overridden to handle just those parameters.
	 * 
	 * @param params The new parameter values, in the same order as that given by getParameterNames().
	 * @param suppressChangeEvent Whether to suppress firing a change event.
	 * @return true iff all parameters were successfully set.
	 */
	public boolean setParameterValues(double[] params, boolean suppressChangeEvent) {
		String[] parameterLabels = getParameterNames();
		boolean setAny = false;
		boolean setAll = true;
		for (int pi = 0; pi < parameterLabels.length; pi++) {
			boolean success = setParameterValue(parameterLabels[pi], params[pi], true);
			setAny |= success;
			setAll &= success;
		}
		if (!suppressChangeEvent && setAny) {
			fireChangeEvent();
		}
		return setAll;
	}

	/**
	 * Sub-classes can override this method to allow setting a parameter value. The default implementation uses Java's
	 * reflection abilities to set the parameter value on the declared public instance variables, assuming that the
	 * parameter name given is the same as the declared variable name. If the field is not of type double then the given
	 * value will be converted to the type of the field using the following rules: boolean is true iff the value is
	 * greater than 0; for integer types (including byte, char, short, int, and long) the value is rounded using
	 * Math.round() and then cast to the required type; and for float a simple cast is performed. Fields that are of the
	 * object wrapper types are also supported (e.g. Double and Integer).
	 * 
	 * @param param The name of parameter;
	 * @param value The new value.
	 * @param suppressChangeEvent Whether to suppress firing a change event. This is most useful if multiple calls to
	 *            this method will occur.
	 * @return true iff the parameter was successfully set.
	 */
	public boolean setParameterValue(String param, double value, boolean suppressChangeEvent) {
		try {
			Field f = this.getClass().getField(param);
			Class t = f.getType();
			if (t.equals(boolean.class) || t.equals(Boolean.class))
				f.setBoolean(this, value > 0);
			else if (t.equals(byte.class) || t.equals(Byte.class))
				f.setByte(this, (byte) Math.round(value));
			else if (t.equals(char.class) || t.equals(Character.class))
				f.setChar(this, (char) Math.round(value));
			else if (t.equals(short.class) || t.equals(Short.class))
				f.setShort(this, (short) Math.round(value));
			else if (t.equals(int.class) || t.equals(Integer.class))
				f.setInt(this, (int) Math.round(value));
			else if (t.equals(long.class) || t.equals(Long.class))
				f.setLong(this, (long) Math.round(value));
			else if (t.equals(float.class) || t.equals(Float.class))
				f.setFloat(this, (float) value);
			else if (t.equals(double.class) || t.equals(Double.class))
				f.setDouble(this, value);
			else {
				throw new IllegalArgumentException("Unsupported field type, only primitive types and their corresponding wrappers are supported.");
			}

			if (!suppressChangeEvent) {
				fireChangeEvent();
			}
		} catch (Exception e) {
			// Just ignore exceptions thrown by getField and setXXX rather than force calling
			// code to deal with handling exceptions. This is probably not ideal...
			return false;
		}
		return true;
	}

	/**
	 * Returns an array containing the names of all the presets available, which should correspond to the presets given
	 * by {@link #getPresets()}.
	 */
	public abstract String[] getPresetNames();

	/**
	 * Return the configuration preset with the given index (which should generally correspond to the preset names given
	 * by {@link #getPresetNames()}. The returned object should be newly created, rather than one created statically.
	 * 
	 * @see #getPresets()
	 */
	public abstract ComponentConfiguration getPreset(int index);

	/**
	 * Returns an array containing all the presets available, which should correspond to the preset names given by
	 * {@link #getPresetNames()}.
	 * 
	 * @see #getPreset(int)
	 */
	public ComponentConfiguration[] getPresets() {
		if (getPresetNames() != null) {
			ComponentConfiguration[] presets = new ComponentConfiguration[getPresetNames().length];
			for (int i = 0; i < presets.length; i++) {
				presets[i] = getPreset(i);
			}
			return presets;
		}
		return null;
	}

	/**
	 * Returns the index of the preset that matches the given configuration, or -1 if there is no match.
	 */
	public int getMatchingPreset() {
		for (int prsi = 0; prsi < getPresetNames().length; prsi++) {
			if (equals(getPreset(prsi))) {
				return prsi;
			}
		}
		return -1;
	}

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
		listeners.clear();
	}

	/**
	 * Notify all ComponentConfigurationListener that parameter values have changed. This method should be invoked
	 * manually if parameters are set directly (and not via the setParameterValue() method).
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
	
	public String toString() {
		String[] parameterLabels = getParameterNames();
		String out = "Configuration for " + this.getClass().getName() + ":";
		for (int pi = 0; pi < parameterLabels.length; pi++) {
			out += "\n\t" + parameterLabels[pi] + ": " + getParameterValue(parameterLabels[pi]);
		}
		return out;
	}
}