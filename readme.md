# Bain - a neural network simulator

The Bain neural network simulator is designed to meet the following 
requirements:
 - Simulate neural networks at a level of fidelity, with respect to natural 
     neural networks, greater than typical rate-based models used in computer 
     science but lower than biophysical models used in neuroscience.
 - Provide a framework to allow easily plugging in parameterised functional/
     computational models for neurons, synapses and neuromodulators 
     (neuromodulator functionality coming soon). The framework is designed 
     for spiking neuron models and plastic synapses, but can also be used
     for rate-based and fixed-weight models.
 - Support arbitrary topologies.
 - Simulate small to large neural networks (tens of neurons/synapses to 
     millions of neurons/synapses), efficiently and with high performance.
 - Make use of SIMD hardware (eg GPUs) for large networks via OpenCL and 
     Aparapi.
 - Be written in Java.


Aparapi allows writing Java code that follows certain conventions and 
restrictions that it will then turn into OpenCL at run-time. If no OpenCL 
compatible platforms are available then Aparapi falls back to using a Java 
Thread Pool or regular sequential operation automatically. Thus to add a new 
model of a neural network component, one only need extend the appropriate base 
class and implement a few methods, without thinking (very much) about OpenCL, 
thread pools, etcetera.

NOTE: unfortunately limitations in Aparapi severely restrict the efficiency of
SIMD computations. At present Aparapi does not allow communication between
kernels (for example the neuron and synapse kernels), thus the output of all
neurons and all synapses must be transferred from the GPU to the CPU  and back 
again so they can be input into the synapses and neurons respectively for the 
next cycle, mostly negating any performance improvements.


## Building

Bain requires Java 7 or greater. Bain is built with 
[gradle](http://gradle.org). A JAR file can be built from the source files 
with:
```
./gradlew build
```
on *nix systems, or
```
gradlew build
```
on Windows systems. This will create a runnable jar file in build/libs.


## Getting started

Read the API documentation starting with com.ojcoleman.bain.Simulation,
and the references within. API documentation is available at 
http://olivercoleman.github.com/bain/


The latest version is available at, and issues should be posted at,
https://github.com/OliverColeman/bain


This library is being written as part of my PhD: "Evolving plastic neural 
networks for online learning". For more details see http://ojcoleman.com.


"Using all this knowledge as a key, we may possibly unlock the secrets of the
anatomical structure; we may compel the cells and fibres to disclose their
meaning and purpose." Alexander Bain (who first proposed that thoughts and body 
activity resulted from interactions among neurons within the brain), 1873, 
Mind and Body: The Theories of Their Relation, New York: D. Appleton and Company.
Also, "Bain" is just one letter away from "Brain"... ;) 


Bain is licensed under the GNU General Public License v3. A copy of the license
is included in the distribution. Please note that Bain is distributed WITHOUT 
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
FOR A PARTICULAR PURPOSE. Please refer to the license for details.
