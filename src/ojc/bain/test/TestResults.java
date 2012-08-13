package ojc.bain.test;

import java.util.*;

/**
 * Class for storing ojc.bain.test results consisting of arrays of doubles. Each array of doubles is referenced by a String
 * label. A TestResults object may be given a type name to describe what it represents. The TestResults class extends HashMap,
 * so all HashMap methods may be used. Result labels and arrays correspond to the keys and values in the HashMap.
 * 
 * @author Oliver J. Coleman
 */
public class TestResults extends HashMap<String, double[]> {
	private static final long serialVersionUID = 1L;

	String type;

	/**
	 * Creates a new TestResults object.
	 * 
	 * @param type The name of the type of results this TestResults object represents.
	 */
	public TestResults(String type) {
		this.type = type;
	}

	/**
	 * Creates a new TestResults object.
	 */
	public TestResults() {
		this.type = "";
	}

	/**
	 * Get the name of the type of results this TestResults object represents. May be the empty String or null.
	 */
	public String getType() {
		return type;
	}

	/**
	 * Sets the name of the type of results this TestResults object represents. May be the empty String or null.
	 */
	public void setType(String type) {
		this.type = type;
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
	 * @param result The data series, should be an array of length 2 containing the arrays for each dimension of the data
	 *            series, in order of first and second dimension.
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
	 * @param result The data series, should be an array of length 3 containing the arrays for each dimension of the data
	 *            series, in order of first, second and third dimension
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
	 * @param result The data series, should be an array of length N containing the arrays for each dimension of the data series
	 *            in the order given in labels.
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
	 * @return an array of length 2 containing the arrays for each dimension of the data series, in order of first and second
	 *         dimension.
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
	 * @return an array of length 3 containing the arrays for each dimension of the data series, in order of first, second and
	 *         third dimension.
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
