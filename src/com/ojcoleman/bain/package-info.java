/**
<p><strong>Bain - a neural network simulator.</strong></p>

<p>Copyright Oliver J. Coleman, 2012.</p>

<p>NOTE: This is an alpha version, the API may change.</p>

<p>The Bain neural network simulator is designed to meet the following 
requirements:
<ul>
<li>Simulate neural networks at a level of fidelity, with respect to natural 
     neural networks, greater than typical rate-based models used in computer 
     science but lower than biophysical models used in neuroscience.</li>
<li>Provide a framework to allow easily plugging in parameterised functional/
     computational models for neurons, synapses and neuromodulators 
     (neuromodulator functionality coming soon). The framework is designed 
     for spiking neuron models and plastic synapses, but can also be used
     for rate-based and fixed-weight models.</li>
<li>Support arbitrary topologies.</li>
<li>Simulate small to large neural networks (tens of neurons/synapses to 
     millions of neurons/synapses), efficiently and with high performance.</li>
<li>Make use of SIMD hardware (eg GPUs) for large networks via OpenCL and 
     Aparapi.</li>
<li>Be written in Java.</li>
</ul></p>
     
<p>Aparapi allows writing Java code that follows certain conventions and 
restrictions that it will then turn into OpenCL at run-time. If no OpenCL 
compatible platforms are available then Aparapi falls back to using a Java 
Thread Pool or regular sequential operation automatically. Thus to add a new 
model of a neural network component, one only need extend the appropriate base 
class and implement a few methods, without thinking (very much) about OpenCL, 
thread pools, etcetera.</p>

<p>To get started, read the API documentation starting with 
{@link com.ojcoleman.bain.NeuralNetwork}, and the references within.</p>

<p>The latest version is available at, and issues should be posted at,
https://github.com/OliverColeman/bain</p>

<p>Dependencies: JFreeChart >=1.0.14, SwingX >=1.6.3, Aparapi. The first two 
dependencies are only required for the synaptic plasticity testing and
analysis GUI.</p>

<p>This library is being written as part of my PhD: "Evolving plastic neural 
networks for online learning". For more details see http://ojcoleman.com.</p>

<p>
<em>"Using all this knowledge as a key, we may possibly unlock the secrets of the
anatomical structure; we may compel the cells and fibres to disclose their
meaning and purpose."</em> Alexander Bain (who first proposed that thoughts and body 
activity resulted from interactions among neurons within the brain), 1873, 
<em>Mind and Body: The Theories of Their Relation</em>, New York: D. Appleton and Company.
Also, "Bain" is just one letter away from "Brain"... ;) </p>

<p>Bain is licensed under the GNU General Public License v3. A copy of the license
is included in the distribution. Please note that Bain is distributed WITHOUT 
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
FOR A PARTICULAR PURPOSE. Please refer to the license for details.</p>
 */
package com.ojcoleman.bain;