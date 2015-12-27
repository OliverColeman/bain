package com.ojcoleman.bain.misc;

import java.util.*;

/**
 * <p>
 * Class for storing test results consisting of arrays of doubles. Each array of doubles is referenced by a String label. The TestResults class extends HashMap,
 * so all HashMap methods may be used. Result labels and arrays correspond to the keys and values in the HashMap.
 * </p>
 * 
 * <p>
 * TestResults also embeds a Properties object, available via {@link TestResults#getProperties()}, which is generally used to describe the parameters or other
 * details of the test.
 * </p>
 * 
 * @author Oliver J. Coleman
 */
public class TestResults extends HashMap<String, double[]> {
	private static final long serialVersionUID = 1L;

	HashMap<String, Object> testProperties = new HashMap<String, Object>();

	/**
	 * Creates a new TestResults object.
	 */
	public TestResults() {
	}

	/**
	 * Set a test property value, for example a parameter used in a test.
	 */
	public void setProperty(String name, Object property) {
		testProperties.put(name, property);
	}

	/**
	 * Get a test property value, for example a parameter used in a test.
	 */
	public Object getProperty(String name) {
		return testProperties.get(name);
	}

	/**
	 * Get the properties of the test.
	 * 
	 * @return a HashMap object which may be queried or have mappings set on it.
	 */
	public HashMap<String, Object> getProperties() {
		return testProperties;
	}

	/**
	 * Adds a result data series.
	 * 
	 * @param label the reference for the data series.
	 * @param result The data series.
	 */
	public void addResult(String label, double[] result) {
		put(label, result);
	}

	/**
	 * Adds a 2D result data series.
	 * 
	 * @param label1 the reference for the first dimension of the data series.
	 * @param label2 the reference for the second dimension of the data series.
	 * @param result The data series, should be an array of length 2 containing the arrays for each dimension of the data series, in order of first and second
	 *            dimension.
	 */
	public void addResult(String label1, String label2, double[][] result) {
		put(label1, result[0]);
		put(label2, result[1]);
	}

	/**
	 * Adds a 3D result data series.
	 * 
	 * @param label1 the reference for the first dimension of the data series.
	 * @param label2 the reference for the second dimension of the data series.
	 * @param label3 the reference for the third dimension of the data series.
	 * @param result The data series, should be an array of length 3 containing the arrays for each dimension of the data series, in order of first, second and
	 *            third dimension
	 */
	public void addResult(String label1, String label2, String label3, double[][] result) {
		put(label1, result[0]);
		put(label2, result[1]);
		put(label3, result[2]);
	}

	/**
	 * Adds an N-dimensional result data series.
	 * 
	 * @param labels the references for each of the N dimension of the data series.
	 * @param result The data series, should be an array of length N containing the arrays for each dimension of the data series in the order given in labels.
	 */
	public void addResult(String[] labels, double[][] result) {
		for (int l = 0; l < labels.length; l++) {
			put(labels[l], result[l]);
		}
	}

	/**
	 * Retrieve a 1D result series.
	 * 
	 * @param label The reference for the data series.
	 */
	public double[] getResult(String label) {
		return get(label);
	}

	/**
	 * Retrieve a 2D result series.
	 * 
	 * @param label1 the reference for the first dimension of the data series.
	 * @param label2 the reference for the second dimension of the data series.
	 * @return an array of length 2 containing the arrays for each dimension of the data series, in order of first and second dimension.
	 */
	public double[][] getResult(String label1, String label2) {
		double[][] result = new double[2][];
		result[0] = getResult(label1);
		result[1] = getResult(label2);
		return result;
	}

	/**
	 * Retrieve a 3D result series.
	 * 
	 * @param label1 the reference for the first dimension of the data series.
	 * @param label2 the reference for the second dimension of the data series.
	 * @param label3 the reference for the third dimension of the data series.
	 * @return an array of length 3 containing the arrays for each dimension of the data series, in order of first, second and third dimension.
	 */
	public double[][] getResult(String label1, String label2, String label3) {
		double[][] result = new double[3][];
		result[0] = getResult(label1);
		result[1] = getResult(label2);
		result[2] = getResult(label3);
		return result;
	}

	/**
	 * Retrieve an N-dimensional result series.
	 * 
	 * @param labels the references for each of the N dimensions of the data series.
	 * @return an array of length N containing the arrays for each dimension of the data series, in the order given in labels.
	 */
	public double[][] getResult(String[] labels) {
		double[][] result = new double[labels.length][];
		for (int l = 0; l < labels.length; l++) {
			result[l] = getResult(labels[l]);
		}
		return result;
	}

	/**
	 * Returns an iterator over all the result labels.
	 */
	public Set<String> getResultLabels() {
		return keySet();
	}
}
