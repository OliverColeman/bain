package ojc.bain.base;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;

import ojc.bain.NeuralNetwork;
import ojc.bain.misc.*;

import com.amd.aparapi.Kernel;
import com.amd.aparapi.Range;

/**
 * <p>
 * Base class for all collections of specific types of neural network components to be used in a {@link NeuralNetwork}. A type of component (e.g. a ) is
 * contained in a collection so as to allow off-loading parallel computations to a vector processor (eg GPU) using Aparapi: simulation calculations should be
 * performed on arrays of primitives containing the state variables, inputs and outputs for the components in the collection.
 * </p>
 * 
 * <p>
 * Rather than extending from this class directly, generally implementations should extend a more specific collection sub-class, such as
 * {@link ojc.bain.base.NeuronCollection} or {@link ojc.bain.base.SynapseCollection}, which provide some useful functionality and an API for working with that
 * type of component. This allows it to be used alongside other standard collection types and within classes like {@link ojc.bain.NeuralNetwork}.
 * </p>
 * 
 * <p>
 * The computations for a collection should be performed in the run() method, which overrides the com.amd.aparapi.Kernel run() method. The {@link #step()}
 * method invokes com.amd.aparapi.Kernel.execute(size*), which invokes the run() method. See the Aparapi documentation for more details, and sub-classes of
 * NeuronCollection and SynapseCollection for examples. For performance reasons, the step() method invokes execute() with {@link #sizePower2} rather than the
 * actual {@link #size}. This means that a sub-class must either:
 * <ol>
 * <li>force the size to be a power of 2; or</li>
 * <li>add logic to the run() method such that it skips components whose index is &gt;= size; or</li>
 * <li>Add dummy elements to all component state variable, input and output arrays such that they have length sizePower2.</li>
 * </ol>
 * </p>
 * 
 * <p>
 * It is assumed that the Aparapi kernel is set to use explicit buffer management, meaning that the required arrays/buffers must be sent to and retrieved from
 * the execution hardware (eg GPU) manually using get() and put() (defined in com.amd.aparapi.Kernel) as necessary. Sub-classes of this class, including
 * ConfigurableComponentCollection, NeuronCollection and SynapseCollection, do this for the arrays/buffers they declare, which is one reason that when
 * overriding methods such as {@link #init()}, {@link #reset()} and {@link #step()} the super-method should be invoked from within the overriding method. See
 * {@link #ensureOutputsAreFresh()} and {@link #ensureStateVariablesAreFresh()}.
 * </p>
 * 
 * @see <a href="http://aparapi.googlecode.com/">Aparapi home page</a>
 * 
 * @author Oliver J. Coleman
 */
public abstract class ComponentCollection extends Kernel {
	private static HashMap<String, ComponentCollection> typeSingletons = new HashMap<String, ComponentCollection>();

	/**
	 * The number of components in this collection.
	 */
	protected int size;

	/**
	 * A number &gt;= to {@link #size} that is a power of 2.
	 */
	protected int sizePower2;

	/**
	 * The current output values of the components.
	 */
	protected double[] outputs;

	/**
	 * The Aparapi execution range.
	 */
	protected Range executeRange;

	/**
	 * The containing network.
	 */
	protected NeuralNetwork network;

	/**
	 * Flag to indicate if the state variables used in an Aparapi kernel have been modified on the GPU (and so would need to be transferred back if we're
	 * interested in looking at their values). This is only relevant when explicit buffer management is being used.
	 */
	protected boolean stateVariablesStale;

	/**
	 * Flag to indicate if the outputs as calculated in an Aparapi kernel have been modified on the GPU (and so would need to be transferred back if we're
	 * interested in looking at their values). This is only relevant when explicit buffer management is being used.
	 * 
	 * @see #ensureOutputsAreFresh()
	 * @see #outputsAreStale()
	 */
	protected boolean outputsStale;

	/**
	 * Flag to indicate if the output values have been manually modified since the last simulation step.
	 */
	protected boolean outputsModified;

	/**
	 * Flag to indicate if the inputs as calculated in an Aparapi kernel have been modified on the GPU (and so would need to be transferred back if we're
	 * interested in looking at their values). This is only relevant when explicit buffer management is being used.
	 * 
	 * @see #ensureInputsAreFresh()
	 * @see #inputsAreStale()
	 */
	protected boolean inputsStale;

	public int getSize() {
		return size;
	}

	/**
	 * Get the containing network.
	 */
	public NeuralNetwork getNetwork() {
		return network;
	}

	/**
	 * Set the containing network. Causes this collection to be reinitialised (via init()), and reset (via reset()).
	 */
	public void setNetwork(NeuralNetwork network) {
		this.network = network;
		init();
		reset();
	}

	/**
	 * Initialise the collection. This method should be called from a sub-classes constructor. Sub-classes should override this method to generate
	 * pre-calculated values used during the simulation, and call this super-method. Arrays/buffers initialised here and used in the run() method/kernel should
	 * be transferred to the execution hardware using put().
	 */
	public void init() {
		setExplicit(true);
		outputsModified = false;
		sizePower2 = Math.max(1, (2 << Utility.log2int(size - 1)));
	}

	/**
	 * Reset the components to their initial state. Sub-classes should override this method if they have state variables that may be reset to an initial state.
	 * Arrays/buffers reset here and used in the run() method/kernel should be transferred to the execution hardware using put().
	 */
	public void reset() {
		Arrays.fill(outputs, 0);
		outputsModified = true;
		outputsStale = false;
	}

	/**
	 * Update the model over one time step. Sub-classes should call this super method. The step() method will invoke the overridden run() method which defines
	 * the Aparapi kernel. This method sets {@link #stateVariablesStale} and {@link #outputsStale} to true.
	 */
	public void step() {
		if (outputsModified) {
			put(outputs);
		}
		execute(executeRange);
		stateVariablesStale = true;
		outputsStale = true;
		inputsStale = true;
	}

	/**
	 * Returns the output of the specified component for the last time step.
	 */
	public double getOutput(int index) {
		ensureOutputsAreFresh();
		return outputs[index];

	}

	/**
	 * Returns a reference to the internal output value array, to allow efficient getting and setting of output values. <strong>If reading values from the
	 * returned array, this method must be called after every call to {@link ojc.bain.NeuralNetwork#step()} or {@link ojc.bain.NeuralNetwork#run(int)}.</strong>
	 * This will ensure that the current values are retrieved from the SIMD hardware (eg GPU) if necessary. <strong>If setting values in the returned array the
	 * method {@link #setOutputsModified()} must be called.</strong>. This will ensure that the modified values are pushed to the SIMD hardware if necessary
	 * during the next simulation step.
	 */
	public double[] getOutputs() {
		ensureOutputsAreFresh();
		return outputs;
	}

	/**
	 * Set current strength (weight) value.
	 * 
	 * @param index The index of the synapse to set the efficacy of.
	 * @param newOutput The new efficacy value.
	 */
	public void setOutput(int index, double newOutput) {
		ensureOutputsAreFresh();
		outputs[index] = newOutput;
		outputsModified = true;
	}

	/**
	 * Returns the input value of the specified component for the next time step.
	 */
	public abstract double getInput(int index);

	/**
	 * Returns the underlying array of input values. This method is provided for efficiency reasons, the values of the array should not be altered directly. The
	 * values in the array returned by this method may become stale if the step() method is invoked subsequently; to get fresh values this method should be
	 * invoked again (this will return the same array but will also ensure that the values in the array are up to date by invoking ensureInputsAreFresh()).
	 */
	public abstract double[] getInputs();

	/**
	 * Add input to the specified component. The given input is added onto the existing input value.
	 */
	public abstract void addInput(int index, double input);

	/**
	 * Ensures the outputs, as provided by {@link #getOutput(int index)} and {@link #getOutputs()} have been fetched from the remote execution hardware (eg GPU)
	 * if necessary. See {@link #outputsAreStale()}.
	 */
	public void ensureOutputsAreFresh() {
		if (outputsStale) {
			get(outputs);
			outputsStale = false;
		}
	}

	/**
	 * Returns true iff the output values of the components, provided by {@link #getOutput(int index)} and {@link #getOutputs()} have become stale due to being
	 * altered on remote execution hardware (eg GPU) and not yet being fetched back.
	 */
	public boolean outputsAreStale() {
		return outputsStale;
	}

	/**
	 * @return true iff the outputs have been manually modified since the last simulation step.
	 */
	public boolean outputsModified() {
		return outputsModified;
	}

	/**
	 * This must be called if the outputs have been manually modified since the last simulation step.
	 */
	public void setOutputsModified() {
		outputsModified = true;
	}

	/**
	 * Ensures the inputs, as provided by {@link #getInput(int index)} and {@link #getInputs()} have been fetched from the remote execution hardware (eg GPU) if
	 * necessary. See {@link #inputsAreStale()}.
	 */
	public abstract void ensureInputsAreFresh();

	/**
	 * Returns true iff the input values of the components, provided by {@link #getInput(int index)} and {@link #getInputs()} have become stale due to being
	 * altered on remote execution hardware (eg GPU) and not yet being fetched back.
	 */
	public boolean inputsAreStale() {
		return inputsStale;
	}

	/**
	 * Returns true iff the state variables of the components, provided by {@link #getStateVariableValues(int index)} have become stale due to being altered on
	 * remote execution hardware (eg GPU) and not yet being fetched back.
	 */
	public boolean stateVariablesAreStale() {
		return stateVariablesStale;
	}

	/**
	 * Ensure the state variable values have been fetched from the remote execution hardware (eg GPU) if necessary. See {@link #stateVariablesStale}.
	 */
	public abstract void ensureStateVariablesAreFresh();

	/**
	 * Get an array containing the names of all the internal state variables. Sub-classes may override this to allow the component to be used for testing or
	 * educational purposes.
	 */
	public String[] getStateVariableNames() {
		return null;
	}

	/**
	 * Get an array containing the values of all the internal state variables, in the same order as that given by getStateVariableNames(). Sub-classes may
	 * override this to allow the component to be used for testing or educational purposes. The overriding method should ensure the state variables are up to
	 * date by invoking {@link #ensureStateVariablesAreFresh()}.
	 */
	public double[] getStateVariableValues(int componentIndex) {
		return null;
	}

	/**
	 * Returns the lowest possible output value for components in this collection. The default implementation returns 0, sub-classes should override this if
	 * necessary.
	 */
	public double getMinimumPossibleOutputValue() {
		return 0;
	}

	/**
	 * Returns the largest possible output value for components in this collection. The default implementation returns 1, sub-classes should override this if
	 * necessary.
	 */
	public double getMaximumPossibleOutputValue() {
		return 1;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This overridden method ensures that when SEQ mode is specified a valid execution Range, with group size == 1, is used.
	 * </p>
	 */
	@Override
	public void setExecutionMode(Kernel.EXECUTION_MODE mode) {
		super.setExecutionMode(mode);
		executeRange = mode == Kernel.EXECUTION_MODE.SEQ ? Range.create(size, 1) : Range.create(size);
	}

	/**
	 * Calling this method makes the specified component type available in the list of component types given by getComponentTypes() and the
	 * getComponentCollectionSingleton() method. Subsequent calls with the same class name have no effect.
	 * 
	 * @param className The class of the Synapse type.
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 */
	public static void registerComponentCollectionType(String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		if (!typeSingletons.containsKey(className)) {
			// Create a singleton for the specific collection with size 0.
			typeSingletons.put(className, ((Class<? extends ComponentCollection>) Class.forName(className)).getConstructor(new Class[] { int.class }).newInstance(new Integer(0)));
		}
	}

	public static String[] getComponentTypes() {
		return typeSingletons.keySet().toArray(new String[typeSingletons.size()]);
	}

	public static ComponentCollection getComponentCollectionSingleton(String className) {
		return typeSingletons.get(className);
	}

	/**
	 * Create a new collection of this type of the given size. Implementations of this method should call {@link #init()}.
	 * 
	 * @param size The size of the new collection.
	 */
	public abstract ComponentCollection createCollection(int size);

	/**
	 * Create a new collection of the given type and size.
	 * 
	 * @param className The class name of the collection to create.
	 * @param size The size of the new collection.
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public static ComponentCollection createCollection(String className, int size) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		registerComponentCollectionType(className);
		return getComponentCollectionSingleton(className).createCollection(size);
	}
}
