/**
 * <p><em><strong>Bain - a neural network simulator.</strong></em></p>
 * <p>Bain is designed to simulate large neural network models at a level of fidelity, with respect to natural neural  
 * networks, greater than typical rate-based models used in computer science but lower than biophysical models used in
 * neuroscience. It aims to provide a framework to allow plugging in parameterised functional/computational models 
 * for neurons, synapses and neuromodulators.</p>
 * <p>Bain is also designed to make use of <a href="http://en.wikipedia.org/wiki/SIMD">SIMD</a> hardware 
 * (eg GPUs), via <a href="http://www.khronos.org/opencl/">OpenCL</a> and 
 * <a href="http://aparapi.googlecode.com/">Aparapi</a>, thus by "large" in the previous paragraph we mean thousands to 
 * millions of neurons and synapses (although it will work equally well with small networks just using the CPU). 
 * Aparapi allows writing Java code that follows certain conventions and restrictions
 * that it will then turn into OpenCL at run-time. If no OpenCL compatible platforms are available then Aparapi falls 
 * back to using a Java Thread Pool automatically. Thus to add a new model of a neural network component, one only need extend 
 * the appropriate base class and implement a few methods, without thinking (very much) about OpenCL, thread pools, 
 * etcetera.</p>
 * <p>To get started, read the API documentation for {@link ojc.bain.Simulation} and the references within.
 * </p> 
 * <p>
 * <em><q>Using all this knowledge as a key, we may possibly unlock the secrets of the anatomical structure; we may compel
 * the the cells and fibres to disclose their meaning and purpose.</q></em> Alexander Bain (1873), <em>Mind and Body: The 
 * Theories of Their Relation</em>, New York: D. Appleton and Company. Also, "Bain" is just one letter away from "Brain"... ;)
 </p>
 */
package ojc.bain;

