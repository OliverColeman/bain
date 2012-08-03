package test;

import base.Simulation;
import neuron.*;
import synapse.*;
import misc.*;

/**
 * Class to test the behaviour of a Synapse implementation on a specific spiking protocol or spiking protocols with timing adjustments.
 * @author Oliver J. Coleman
 */
public class STDPTest {
	public static final String RESULT_TYPE_STDP = "STDP";
	public static final String RESULT_TYPE_STDP_1D = "STDP 1D";
	public static final String RESULT_TYPE_STDP_2D = "STDP 2D";
	
	/*
	 * @param synapse The Synapse to test.
	 * @param period The period of the spike pattern in milliseconds.
	 * @param repetitions The number of times to apply the spike pattern.
	 * @param patterns Array containing spike patterns, in the form [initial, dim 1, dim 2][pre, post][spike number] = spike time. The [spike number] array contains the times (ms) of each spike, relative to the beginning of the pattern.
	 * @param refSpikeIndexes Array specifying indexes of the two spikes to use as timing variation references for each variation dimension, in the form [dim 1, dim 2][reference spike, relative spike] = spike index.
	 * @param refSpikePreOrPost Array specifying whether the timing variation reference spikes specified by refSpikeIndexes belong to the pre- or post-synaptic neurons, in the form [dim 1, dim 2][base spike, relative spike] = Constants.PRE or Constants.POST.
	 * @param results Container for results.
	 */ 
	public static TestResults testPattern(Synapse synapse, int timeResolution, double period, int repetitions, double[][][] patterns, int[][] refSpikeIndexes, int[][] refSpikePreOrPost, boolean logSpikesAndStateVariables) throws IllegalArgumentException {
		int variationDimsCount = patterns.length-1; //Number of dimensions over which spike timing patterns vary.
		if (variationDimsCount > 2) {
			throw new IllegalArgumentException("The number of variation dimensions may not exceed 2 (patterns.length must be <= 3)");
		}
		
		TestResults results = new TestResults();
		
		Simulation sim = new Simulation(timeResolution);
		Simulation.setSingleton(sim);
		
		NeuronFixed pre = new NeuronFixed(new NeuronFixedConfig(period, patterns[0][0]));
		NeuronFixed post = new NeuronFixed(new NeuronFixedConfig(period, patterns[0][1]));
		synapse.setPre(pre);
		synapse.setPost(post);
		
		sim.addNeuron(pre);
		sim.addNeuron(post);
		sim.addSynapse(synapse);
		
		int simSteps = (int) Math.round(period*repetitions*timeResolution);
		
		int displayTimeResolution = Math.min(1000, timeResolution);
		
		long startTime = System.currentTimeMillis();
		
		// If we're just testing a single spike pattern.
		// Handle separately as logging is quite different from testing spike patterns with gradually altered spike times.
		if (variationDimsCount == 0) {
			int logStepCount = timeResolution / displayTimeResolution;
			int logSize = simSteps / logStepCount;
			
			double[] timeLog = new double[logSize];
			double[] strengthLog = new double[logSize];
			double[][] prePostLogs = null, traceLogs = null;
			double[] stateVars;
			if (logSpikesAndStateVariables) {
				prePostLogs = new double[2][logSize];
				traceLogs = new double[synapse.getStateVariableNames().length][logSize];
			}
			
			int logIndex = 0;
			for (int step = 0; step < simSteps; step++) {
				double time = sim.getTime();
				sim.step();
				
				if (step % logStepCount == 0) {
					timeLog[logIndex] = time;
					strengthLog[logIndex] = synapse.getStrength();
					// If we're only testing a few repetitions, include some extra data.
					if (logSpikesAndStateVariables) {
						prePostLogs[0][logIndex] = pre.getOutput();
						prePostLogs[1][logIndex] = post.getOutput();
						stateVars = synapse.getStateVariableValues();
						for (int v = 0; v < stateVars.length; v++) {
							traceLogs[v][logIndex] = stateVars[v];
						}
					}
					logIndex++;
				}
			}
			
			results.setType(RESULT_TYPE_STDP);
			results.addResult("Strength", strengthLog);
			results.addResult("Time", timeLog);
			if (logSpikesAndStateVariables) {
				results.addResult("Pre-synaptic spikes", prePostLogs[0]);
				results.addResult("Post-synaptic spikes", prePostLogs[1]);
				String[] stateVariableNames = synapse.getStateVariableNames();
				for (int v = 0; v < stateVariableNames.length; v++) {
					results.addResult(stateVariableNames[v], traceLogs[v]);
				}
			}
		}
		
		else { // We're testing spike patterns with gradually altered spike times over one or two dimensions.
			
			int[] spikeCounts = {patterns[0][0].length, patterns[0][1].length};
			double[] timeDeltaInitial = new double[2], timeDeltaFinal = new double[2]; // The initial and final time deltas (s), given base and relative spike times in initial and final spike patterns, for each variation dimension. 
			double[] timeDeltaRange = new double[2]; // The time delta range (s) for each variation dimension.
			int[] positionsCount = new int[2];
			int[][] variationDimForSpike = new int[2][Math.max(spikeCounts[0], spikeCounts[1])]; //[pre, post][spike index]
			
			for (int d = 0; d < variationDimsCount; d++) {
				double baseRefSpikeTimeInitial = patterns[0][refSpikePreOrPost[d][0]][refSpikeIndexes[d][0]];
				double relativeRefSpikeTimeInitial = patterns[0][refSpikePreOrPost[d][1]][refSpikeIndexes[d][1]];
				double baseRefSpikeTimeFinal = patterns[d+1][refSpikePreOrPost[d][0]][refSpikeIndexes[d][0]];
				double relativeRefSpikeTimeFinal = patterns[d+1][refSpikePreOrPost[d][1]][refSpikeIndexes[d][1]];
				
				timeDeltaInitial[d] = relativeRefSpikeTimeInitial - baseRefSpikeTimeInitial;
				timeDeltaFinal[d] = relativeRefSpikeTimeFinal - baseRefSpikeTimeFinal;
				timeDeltaRange[d] = Math.abs(timeDeltaInitial[d] - timeDeltaFinal[d]);
				
				// From the initial and final spiking protocols we generate intermediate spiking protocols by interpolation.
				// Each position in between the initial and final protocol adjusts the time differential between the base and reference spikes by (1/timeResolution) seconds.
				positionsCount[d] = (int) Math.round(timeDeltaRange[d]*displayTimeResolution) + 1;
				
				// Determine which dimension, if any, a spikes timing varies over (and ensure that a spikes timing only varies over at most one dimension).
				for (int p = 0; p < 2; p++) {
					for (int si = 0; si < spikeCounts[p]; si++) {
						// If the spikes time in variation dimension d is different to the initial spike time. 
						if (patterns[0][p][si] != patterns[d+1][p][si]) {
							// If it also differs in another dimension.
							if (variationDimForSpike[p][si] != 0) {
								throw new IllegalArgumentException("A spikes timing may vary at most over one variation dimension. " + (p == 0 ? "Pre" : "Post") + "-synaptic spike " + (si + 1) + " varies over two.");
							}
							variationDimForSpike[p][si] = d+1;
						}
					}
				}
			}
			
			double[][] currentSpikeTimings = new double[2][]; // Current pre and post spiking patterns [pre, post][spike index]
			for (int p = 0; p < 2; p++) {
				currentSpikeTimings[p] = new double[spikeCounts[p]];
				System.arraycopy(patterns[0][p], 0, currentSpikeTimings[p], 0, spikeCounts[p]);
			}
			
			if (variationDimsCount == 1) { // We're testing spike patterns with gradually altered spike times over one dimension.
				// Arrays to record results.
				double[] time = new double[positionsCount[0]];  // The time delta in seconds [time delta index]
				double[] strengthLog = new double[positionsCount[0]]; //The change in synapse strength after all repetitions for each pattern [time delta index] 
				
				for (int timeDeltaIndex = 0; timeDeltaIndex < positionsCount[0]; timeDeltaIndex++) {
					double position = (double) timeDeltaIndex / (positionsCount[0]-1); // Position in variation dimension 1
					
					// Generate pre and post spike timing patterns for this position.
					for (int p = 0; p < 2; p++) {
						for (int si = 0; si < spikeCounts[p]; si++) {
							// If this spikes timing varies.
							int variationDim = variationDimForSpike[p][si];
							if (variationDim != 0) {
								currentSpikeTimings[p][si] = position * patterns[0][p][si] + (1 - position) * patterns[variationDim][p][si];
							}
						}
					}
					
					((NeuronFixedConfig) pre.getConfig()).setSpikeTimings(currentSpikeTimings[0]);
					((NeuronFixedConfig) post.getConfig()).setSpikeTimings(currentSpikeTimings[1]);
					sim.reset();
					for (int step = 0; step < simSteps; step++) {
						sim.step();
					}
					time[timeDeltaIndex] = position * timeDeltaInitial[0] + (1 - position) * timeDeltaFinal[0];
					strengthLog[timeDeltaIndex] = synapse.getStrength();
				}
				
				results.setType(RESULT_TYPE_STDP_1D);
				results.addResult("Strength", strengthLog);
				results.addResult("Time delta", time);
			}
			
			else { // We're testing spike patterns with gradually altered spike times over two dimensions.
				// Array to record results.
				double[][] strengthLog = new double[3][(positionsCount[0]+1) * (positionsCount[1]+1)]; //The change in synapse strength after all repetitions for each pattern [time delta for var dim 1, time delta for var dim 2, synapse strength][result index]
				
				double[] position = new double[2]; //Position in variation dimensions 1 and 2
				for (int timeDeltaIndex1 = 0, resultIndex = 0; timeDeltaIndex1 < positionsCount[0]; timeDeltaIndex1++) {
					position[0] = (double) timeDeltaIndex1 / positionsCount[0]; // Position in variation dimension 1
					
					for (int timeDeltaIndex2 = 0; timeDeltaIndex2 < positionsCount[1]; timeDeltaIndex2++, resultIndex++) {
						position[1] = (double) timeDeltaIndex2 / positionsCount[1]; // Position in variation dimension 2
						
						// Generate pre and post spike timing patterns for this position.
						for (int p = 0; p < 2; p++) {
							for (int si = 0; si < spikeCounts[p]; si++) {
								// If this spikes timing varies.
								int variationDim = variationDimForSpike[p][si];
								if (variationDim != 0) {
									currentSpikeTimings[p][si] = (1 - position[variationDim-1]) * patterns[0][p][si] + position[variationDim-1] * patterns[variationDim][p][si];
								}
							}
						}
						
						((NeuronFixedConfig) pre.getConfig()).setSpikeTimings(currentSpikeTimings[0]);
						((NeuronFixedConfig) post.getConfig()).setSpikeTimings(currentSpikeTimings[1]);
						sim.reset();
						for (int step = 0; step < simSteps; step++) {
							sim.step();
						}
						
						strengthLog[0][resultIndex] = (1 - position[0]) * timeDeltaInitial[0] + position[0] * timeDeltaFinal[0];
						strengthLog[1][resultIndex] = (1 - position[1]) * timeDeltaInitial[1] + position[1] * timeDeltaFinal[1];
						strengthLog[2][resultIndex] = synapse.getStrength();
					}
				}
				
				results.setType(RESULT_TYPE_STDP_2D);
				results.addResult("Time delta 1", "Time delta 2", "Strength", strengthLog);
			}
		}
		
		long finishTime = System.currentTimeMillis();
		System.out.println("Took " + ((finishTime-startTime)/1000f) + "s");
			
		return results;
	}
	
}