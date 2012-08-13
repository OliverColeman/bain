Bain - a neural network simulator.

Copyright Oliver J. Coleman, 2012.

NOTE: This is an alpha version, it is not quite functional yet 
(the synapse testing and analysis GUI is, though: ojc.bain.test.STDPTestGUI).

Bain is designed to simulate large neural network models at a level of fidelity,
with respect to natural neural networks, greater than typical rate-based models
used in computer science but lower than biophysical models used in neuroscience.
It aims to provide a framework to allow plugging in parameterised
functional/computational models for neurons, synapses and neuromodulators.

Bain is designed to make use of SIMD hardware (eg GPUs), via OpenCL and
Aparapi, thus by "large" in the previous paragraph we mean thousands to millions
of neurons and synapses (although it will work equally well with small networks
just using the CPU). Aparapi allows writing Java code that follows certain
conventions and restrictions that it will then turn into OpenCL at run-time. If
no OpenCL compatible platforms are available then Aparapi falls back to using a
Java Thread Pool automatically. Thus to add a new model of a neural network
component, one only need extend the appropriate base class and implement a few
methods, without thinking (very much) about OpenCL, thread pools, etcetera.

To get started, read the API documentation starting with ojc.bain.Simulation,
and the references within.

The latest version is available at, and issues should be posted at,
https://github.com/OliverColeman/bain

Dependencies: JFreeChart >=1.0.14, SwingX >=1.6.3, Aparapi. The first two 
dependencies are only required for the synaptic plasticity testing and
analysis GUI.

This library is being written as part of my PhD: "Evolving plastic neural 
networks for online learning". For more details see http://ojcoleman.com.


"Using all this knowledge as a key, we may possibly unlock the secrets of the
anatomical structure; we may compel the the cells and fibres to disclose their
meaning and purpose." Alexander Bain (1873), Mind and Body: The Theories of
Their Relation, New York: D. Appleton and Company. Also, "Bain" is just one
letter away from "Brain"... ;) 

Bain is licensed under the GNU General Public License v3. A copy of the license
is included in the distribution. Please note that Bain is distributed WITHOUT 
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
FOR A PARTICULAR PURPOSE. Please refer to the license for details.
