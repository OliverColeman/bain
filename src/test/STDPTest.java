package test;

import neuron.*;
import synapse.*;
import misc.*;

import java.util.*;

import javax.swing.JOptionPane;

import org.jfree.data.xy.*;

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
	public static TestResults testPattern(Synapse synapse, int period, int repetitions, int[][][] patterns, int[][] refSpikeIndexes, int[][] refSpikePreOrPost, boolean logSpikesAndStateVariables) throws IllegalArgumentException {
		int variationDimsCount = patterns.length-1; //Number of dimensions over which spike timing patterns vary.
		if (variationDimsCount > 2) {
			throw new IllegalArgumentException("The number of variation dimensions may not exceed 2 (patterns.length must be <= 3)");
		}
		
		TestResults results = new TestResults();
		
		NeuronFixed pre = new NeuronFixed(period, patterns[0][0]);
		NeuronFixed post = new NeuronFixed(period, patterns[0][1]);
		synapse.setPre(pre);
		synapse.setPost(post);
		synapse.reset();
		
		// If we're just testing a single spike pattern.
		// Handle separately as logging is quite different from testing spike patterns with gradually altered spike times.
		if (variationDimsCount == 0) {
			double[] time = new double[period*repetitions];
			double[] strengthLog = new double[period*repetitions];
			double[][] prePostLogs = null, traceLogs = null;
			double[] stateVars;
			if (repetitions <= 5) {
				prePostLogs = new double[2][period*repetitions];
				traceLogs = new double[synapse.getStateVariableNames().length][period*repetitions];
			}
			
			for (int step = 0; step < (repetitions * period); step++) {
				pre.step();
				post.step();
				synapse.step();
				
				time[step] = step / 1000d;
				strengthLog[step] = synapse.getStrength();
				
				// If we're only testing a few repetitions, include some extra data.
				if (logSpikesAndStateVariables) {
					prePostLogs[0][step] = pre.getOutput();
					prePostLogs[1][step] = post.getOutput();
					stateVars = synapse.getStateVariableValues();
					for (int v = 0; v < stateVars.length; v++) {
						traceLogs[v][step] = stateVars[v];
					}
				}
			}
			
			results.setType(RESULT_TYPE_STDP);
			results.addResult("Strength", strengthLog);
			results.addResult("Time", time);
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
			int[] timeDeltaInitial = new int[2], timeDeltaFinal = new int[2]; // The initial and final time deltas (ms), given base and relative spike times in initial and final spike patterns, for each variation dimension. 
			int[] timeDeltaRange = new int[2]; // The time delta range (ms) for each variation dimension.
			int[][] variationDimForSpike = new int[2][Math.max(spikeCounts[0], spikeCounts[1])]; //[pre, post][spike index]
			
			for (int d = 0; d < variationDimsCount; d++) {
				int baseRefSpikeTimeInitial = patterns[0][refSpikePreOrPost[d][0]][refSpikeIndexes[d][0]];
				int relativeRefSpikeTimeInitial = patterns[0][refSpikePreOrPost[d][1]][refSpikeIndexes[d][1]];
				int baseRefSpikeTimeFinal = patterns[d+1][refSpikePreOrPost[d][0]][refSpikeIndexes[d][0]];
				int relativeRefSpikeTimeFinal = patterns[d+1][refSpikePreOrPost[d][1]][refSpikeIndexes[d][1]];
				
				timeDeltaInitial[d] = relativeRefSpikeTimeInitial - baseRefSpikeTimeInitial;
				timeDeltaFinal[d] = relativeRefSpikeTimeFinal - baseRefSpikeTimeFinal;
				timeDeltaRange[d] = Math.abs(timeDeltaInitial[d] - timeDeltaFinal[d]);
				
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
			
			int[][] currentSpikeTimings = new int[2][]; // Current pre and post spiking patterns [pre, post][spike index]
			for (int p = 0; p < 2; p++) {
				currentSpikeTimings[p] = new int[spikeCounts[p]];
				System.arraycopy(patterns[0][p], 0, currentSpikeTimings[p], 0, spikeCounts[p]);
			}
			
			if (variationDimsCount == 1) { // We're testing spike patterns with gradually altered spike times over one dimension.
				// Arrays to record results.
				double[] time = new double[timeDeltaRange[0]+1];  // The time delta in seconds [time delta index]
				double[] strengthLog = new double[timeDeltaRange[0]+1]; //The change in synapse strength after all repetitions for each pattern [time delta index] 
				
				for (int timeDeltaIndex = 0; timeDeltaIndex <= timeDeltaRange[0]; timeDeltaIndex++) {
					float position = (float) timeDeltaIndex / timeDeltaRange[0]; // Position in variation dimension 1
					
					// Generate pre and post spike timing patterns for this position.
					for (int p = 0; p < 2; p++) {
						for (int si = 0; si < spikeCounts[p]; si++) {
							// If this spikes timing varies.
							int variationDim = variationDimForSpike[p][si];
							if (variationDim != 0) {
								currentSpikeTimings[p][si] = Math.round(position * patterns[0][p][si] + (1 - position) * patterns[variationDim][p][si]);
							}
						}
					}
					
					pre.setSpikeTimings(currentSpikeTimings[0]);
					post.setSpikeTimings(currentSpikeTimings[1]);
					synapse.reset();
					for (int step = 0; step < (repetitions * period); step++) {
						pre.step();
						post.step();
						synapse.step();
					}
					time[timeDeltaIndex] = Math.round(position * timeDeltaInitial[0] + (1 - position) * timeDeltaFinal[0]) / 1000d;
					strengthLog[timeDeltaIndex] = synapse.getStrength();
				}
				
				results.setType(RESULT_TYPE_STDP_1D);
				results.addResult("Strength", strengthLog);
				results.addResult("Time delta", time);
			}
			
			else { // We're testing spike patterns with gradually altered spike times over two dimensions.
				// Array to record results.
				double[][] strengthLog = new double[3][(timeDeltaRange[0]+1) * (timeDeltaRange[1]+1)]; //The change in synapse strength after all repetitions for each pattern [time delta for var dim 1, time delta for var dim 2, synapse strength][result index]
				
				float[] position = new float[2]; //Position in variation dimensions 1 and 2
				for (int timeDeltaIndex1 = 0, resultIndex = 0; timeDeltaIndex1 <= timeDeltaRange[0]; timeDeltaIndex1++) {
					position[0] = (float) timeDeltaIndex1 / timeDeltaRange[0]; // Position in variation dimension 1
					
					for (int timeDeltaIndex2 = 0; timeDeltaIndex2 <= timeDeltaRange[1]; timeDeltaIndex2++, resultIndex++) {
						position[1] = (float) timeDeltaIndex2 / timeDeltaRange[1]; // Position in variation dimension 2
						
						// Generate pre and post spike timing patterns for this position.
						for (int p = 0; p < 2; p++) {
							for (int si = 0; si < spikeCounts[p]; si++) {
								// If this spikes timing varies.
								int variationDim = variationDimForSpike[p][si];
								if (variationDim != 0) {
									currentSpikeTimings[p][si] = Math.round((1 - position[variationDim-1]) * patterns[0][p][si] + position[variationDim-1] * patterns[variationDim][p][si]);
								}
							}
						}
						
						//System.out.println(position[0] + " , " + position[1] + " : " + Arrays.deepToString(currentSpikeTimings));
						pre.setSpikeTimings(currentSpikeTimings[0]);
						post.setSpikeTimings(currentSpikeTimings[1]);
						synapse.reset();
						for (int step = 0; step < (repetitions * period); step++) {
							pre.step();
							post.step();
							synapse.step();
						}
						
						strengthLog[0][resultIndex] = Math.round((1 - position[0]) * timeDeltaInitial[0] + position[0] * timeDeltaFinal[0]);// / 1000d;
						strengthLog[1][resultIndex] = Math.round((1 - position[1]) * timeDeltaInitial[1] + position[1] * timeDeltaFinal[1]);// / 1000d;
						strengthLog[2][resultIndex] = synapse.getStrength();
						
						//System.out.println(strengthLog[0][resultIndex] + " , " + strengthLog[1][resultIndex] + " : " + strengthLog[2][resultIndex]);
					}
				}
				
				//System.out.println();
				//System.out.println(Arrays.toString(strengthLog[0]));
				//System.out.println(Arrays.toString(strengthLog[1]));
				//System.out.println(Arrays.toString(strengthLog[2]));
				
				results.setType(RESULT_TYPE_STDP_2D);
				results.addResult("Time delta 1", "Time delta 2", "Strength", strengthLog);
			}
		}
			
		return results;
	}
	
}