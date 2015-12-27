package com.ojcoleman.bain.misc;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.Arrays;

import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.ProgressMonitor;


import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.labels.StandardXYZToolTipGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYBarDataset;
import org.jfree.ui.RectangleEdge;

import com.ojcoleman.bain.NeuralNetwork;
import com.ojcoleman.bain.base.ComponentConfiguration;
import com.ojcoleman.bain.base.SynapseCollection;
import com.ojcoleman.bain.misc.*;
import com.ojcoleman.bain.neuron.*;
import com.ojcoleman.bain.neuron.spiking.FixedProtocolNeuronCollection;
import com.ojcoleman.bain.neuron.spiking.FixedProtocolNeuronConfiguration;
import com.ojcoleman.bain.synapse.*;

/**
 * Class to test the behaviour of synapse models on given spiking protocols.
 * 
 * @author Oliver J. Coleman
 */
public class SynapseTest {
	/**
	 * The type of test results.
	 * 
	 * <ul>
	 * <li>STDP denotes a Spike-Timing-Dependent Plasticity test.</li>
	 * <li>1D and 2D denote a test for which variables are varied over 1 or 2 dimensions respectively.</li>
	 * </ul>
	 */
	public static enum TYPE {
		STDP, STDP_1D, STDP_2D;
	};

	/**
	 * Test the behaviour of a synapse model.
	 * 
	 * @param network A neural network containing two neurons and a single synapse to be tested. The neurons at index 0 and 1 should be the pre- and
	 *            post-synaptic neurons respectively, and are typically configured to produce a fixed firing pattern (though any neuron type and firing pattern
	 *            are permitted).
	 * @param simSteps The number of steps to run the simulation for.
	 * @param logSpikesAndStateVariables Whether to record pre- and post-synaptic spikes and any state variables exposed by the synapse model in the test
	 *            results.
	 * @param simStepsNoSpikes The number of steps to run the simulation for with no spiking after the normal spike test. This is useful for testing models
	 *            which change the efficacy in the absence of spikes.
	 * @return For a single spike protocol, a TestResults object with type {@link TYPE#STDP} consisting of series labelled "Time" and "Efficacy" and if
	 *         logSpikesAndStateVariables == true then also "Pre-synaptic spikes", "Post-synaptic spikes" and any state variables exposed by the synapse model.
	 */
	public static TestResults singleTest(NeuralNetwork network, long simSteps, boolean logSpikesAndStateVariables, long simStepsNoSpikes) {
		if (network.getNeurons().getSize() != 2 || network.getSynapses().getSize() != 1) {
			throw new IllegalArgumentException("The neural network must contain at least 2 neurons and 1 synapse.");
		}

		simStepsNoSpikes = Math.max(0, simStepsNoSpikes);

		int displayTimeResolution = Math.min(1000, network.getTimeResolution());

		int logStepCount = network.getTimeResolution() / displayTimeResolution;
		int logSize = (int) ((simSteps + simStepsNoSpikes) / logStepCount);

		double[] timeLog = new double[logSize];
		double[] efficacyLog = new double[logSize];
		double[][] prePostLogs = null, traceLogs = null;
		double[] stateVars;
		if (logSpikesAndStateVariables) {
			prePostLogs = new double[2][logSize];
			traceLogs = new double[network.getSynapses().getStateVariableNames().length][logSize];
		}

		int logIndex = 0;
		long step = 0;
		for (; step < simSteps; step++) {
			double time = network.getTime();
			network.step();

			if (step % logStepCount == 0) {
				timeLog[logIndex] = time;
				efficacyLog[logIndex] = network.getSynapses().getEfficacy(0);
				// If we're only testing a few repetitions, include some extra
				// data.
				if (logSpikesAndStateVariables) {
					prePostLogs[0][logIndex] = network.getNeurons().getOutput(0);
					prePostLogs[1][logIndex] = network.getNeurons().getOutput(1);
					stateVars = network.getSynapses().getStateVariableValues(0);
					for (int v = 0; v < stateVars.length; v++) {
						traceLogs[v][logIndex] = stateVars[v];
					}
				}
				logIndex++;
			}
		}

		if (simStepsNoSpikes > 0) {
			FixedProtocolNeuronConfiguration config = new FixedProtocolNeuronConfiguration(100, new double[] {});
			network.getNeurons().setConfiguration(0, config);
			network.getNeurons().setComponentConfiguration(0, 0);
			network.getNeurons().setComponentConfiguration(1, 0);

			for (; step < simSteps + simStepsNoSpikes; step++) {
				double time = network.getTime();
				network.step();

				if (step % logStepCount == 0) {
					timeLog[logIndex] = time;
					efficacyLog[logIndex] = network.getSynapses().getEfficacy(0);
					// If we're only testing a few repetitions, include some extra data.
					if (logSpikesAndStateVariables) {
						prePostLogs[0][logIndex] = network.getNeurons().getOutput(0);
						prePostLogs[1][logIndex] = network.getNeurons().getOutput(1);
						stateVars = network.getSynapses().getStateVariableValues(0);
						for (int v = 0; v < stateVars.length; v++) {
							traceLogs[v][logIndex] = stateVars[v];
						}
					}
					logIndex++;
				}
			}
		}

		TestResults results = new TestResults();
		results.setProperty("type", TYPE.STDP);
		results.addResult("Efficacy", efficacyLog);
		results.addResult("Time", timeLog);
		if (logSpikesAndStateVariables) {
			results.addResult("Pre-synaptic spikes", prePostLogs[0]);
			results.addResult("Post-synaptic spikes", prePostLogs[1]);
			String[] stateVariableNames = network.getSynapses().getStateVariableNames();
			for (int v = 0; v < stateVariableNames.length; v++) {
				results.addResult(stateVariableNames[v], traceLogs[v]);
			}
		}

		return results;
	}

	/**
	 * Test a synapse on the specified spiking protocol or a series of spiking protocols derived from initial and final protocols by interpolation over one or
	 * two dimensions.
	 * 
	 * @param synapse The SynapseCollection containing the synapse to test (the first synapse is used).
	 * @param timeResolution The time resolution to use in the simulation, see {@link com.ojcoleman.bain.NeuralNetwork}
	 * @param period The period of the spike pattern in seconds.
	 * @param repetitions The number of times to apply the spike pattern.
	 * @param patterns Array containing spike patterns, in the form [initial, dim 1, dim 2][pre, post][spike number] = spike time. The [spike number] array
	 *            contains the times (s) of each spike, relative to the beginning of the pattern. See
	 *            {@link com.ojcoleman.bain.neuron.spiking.FixedProtocolNeuronCollection}.
	 * @param refSpikeIndexes Array specifying indexes of the two spikes to use as timing variation references for each variation dimension, in the form [dim 1,
	 *            dim 2][reference spike, relative spike] = spike index.
	 * @param refSpikePreOrPost Array specifying whether the timing variation reference spikes specified by refSpikeIndexes belong to the pre- or post-synaptic
	 *            neurons, in the form [dim 1, dim 2][base spike, relative spike] = Constants.PRE or Constants.POST.
	 * @param logSpikesAndStateVariables Whether to record pre- and post-synaptic spikes and any state variables exposed by the synapse model in the test
	 *            results.
	 * @param progressMonitor If not null, this will be updated with the current progress.
	 * @return For a single spike protocol, a TestResults object with type {@link TYPE#STDP} consisting of series labelled "Time" and "Efficacy" and if
	 *         logSpikesAndStateVariables == true then also "Pre-synaptic spikes", "Post-synaptic spikes" and any state variables exposed by the synapse model.
	 *         For a protocol varied over one dimension, a TestResults object with type {@link TYPE#STDP_1D} consisting of series labelled "Time delta" and
	 *         "Efficacy". For a protocol varied over two dimensions, a TestResults object with type {@link TYPE#STDP_2D} consisting of series labelled
	 *         "Time delta 1", "Time delta 2" and "Efficacy".
	 */
	public static TestResults testPattern(SynapseCollection<? extends ComponentConfiguration> synapse, int timeResolution, double period, int repetitions, double[][][] patterns, int[][] refSpikeIndexes, int[][] refSpikePreOrPost, boolean logSpikesAndStateVariables, ProgressMonitor progressMonitor) throws IllegalArgumentException {
		int variationDimsCount = patterns.length - 1; // Number of dimensions over which spike timing patterns vary.
		if (variationDimsCount > 2) {
			throw new IllegalArgumentException("The number of variation dimensions may not exceed 2 (patterns.length must be <= 3)");
		}

		if (progressMonitor != null) {
			progressMonitor.setMinimum(0);
		}

		TestResults results = new TestResults();

		FixedProtocolNeuronCollection neurons = new FixedProtocolNeuronCollection(2);
		FixedProtocolNeuronConfiguration preConfig = new FixedProtocolNeuronConfiguration(period, patterns[0][0]);
		neurons.addConfiguration(preConfig);
		neurons.setComponentConfiguration(0, 0);
		FixedProtocolNeuronConfiguration postConfig = new FixedProtocolNeuronConfiguration(period, patterns[0][1]);
		neurons.addConfiguration(postConfig);
		neurons.setComponentConfiguration(1, 1);

		synapse.setPreNeuron(0, 0);
		synapse.setPostNeuron(0, 1);

		NeuralNetwork sim = new NeuralNetwork(timeResolution, neurons, synapse);

		int simSteps = (int) Math.round(period * repetitions * timeResolution);

		int displayTimeResolution = Math.min(1000, timeResolution);

		results.setProperty("simulation time resolution", timeResolution);
		results.setProperty("display time resolution", displayTimeResolution);

		// long startTime = System.currentTimeMillis();

		// If we're just testing a single spike pattern. // Handle separately as logging is quite different from testing spike
		// patterns with gradually altered spike times.
		if (variationDimsCount == 0) {
			results = singleTest(sim, simSteps, logSpikesAndStateVariables, 0);
		} else { // We're testing spike patterns with gradually altered spike times over one or two dimensions.

			int[] spikeCounts = { patterns[0][0].length, patterns[0][1].length };
			// The initial and final time deltas (s), given base and relative spike times in initial and final spike patterns,
			// for each variation dimension.
			double[] timeDeltaInitial = new double[2], timeDeltaFinal = new double[2];
			// The time delta range(s) for each variation dimension.
			double[] timeDeltaRange = new double[2];
			int[] positionsCount = new int[2];
			int[][] variationDimForSpike = new int[2][Math.max(spikeCounts[0], spikeCounts[1])]; // [pre, post][spike index]

			// Set-up parameters for testing spike patterns with gradually altered spike times over one or two dimensions.
			for (int d = 0; d < variationDimsCount; d++) {
				double baseRefSpikeTimeInitial = patterns[0][refSpikePreOrPost[d][0]][refSpikeIndexes[d][0]];
				double relativeRefSpikeTimeInitial = patterns[0][refSpikePreOrPost[d][1]][refSpikeIndexes[d][1]];
				double baseRefSpikeTimeFinal = patterns[d + 1][refSpikePreOrPost[d][0]][refSpikeIndexes[d][0]];
				double relativeRefSpikeTimeFinal = patterns[d + 1][refSpikePreOrPost[d][1]][refSpikeIndexes[d][1]];

				timeDeltaInitial[d] = relativeRefSpikeTimeInitial - baseRefSpikeTimeInitial;
				timeDeltaFinal[d] = relativeRefSpikeTimeFinal - baseRefSpikeTimeFinal;
				timeDeltaRange[d] = Math.abs(timeDeltaInitial[d] - timeDeltaFinal[d]);

				// From the initial and final spiking protocols we generate intermediate spiking protocols by interpolation. //
				// Each position in between the initial and final protocol adjusts the time differential between the base and
				// reference spikes by (1/timeResolution) seconds.
				positionsCount[d] = (int) Math.round(timeDeltaRange[d] * displayTimeResolution) + 1;

				// Determine which dimension, if any, a spikes timing varies over (and ensure that a spikes timing only varies
				// over at most one dimension).
				// If the spikes time in variation dimension d is different to the initial spike time.
				for (int p = 0; p < 2; p++) {
					for (int si = 0; si < spikeCounts[p]; si++) {
						// If it also differs in another dimension.
						if (patterns[0][p][si] != patterns[d + 1][p][si]) {
							if (variationDimForSpike[p][si] != 0) {
								throw new IllegalArgumentException("A spikes timing may vary at most over one variation dimension. " + (p == 0 ? "Pre" : "Post") + "-synaptic spike " + (si + 1) + " varies over two.");
							}
							variationDimForSpike[p][si] = d + 1;
						}
					}
				}
			}

			double[][] currentSpikeTimings = new double[2][]; // Current pre and post spiking patterns [pre, post][spike index]
			for (int p = 0; p < 2; p++) {
				currentSpikeTimings[p] = new double[spikeCounts[p]];
				System.arraycopy(patterns[0][p], 0, currentSpikeTimings[p], 0, spikeCounts[p]);
			}

			// If we're testing spike patterns with gradually altered spike times over one dimension.
			if (variationDimsCount == 1) {
				// Arrays to record results.
				double[] time = new double[positionsCount[0]]; // The time delta in seconds [time delta index]
				// The change in synapse efficacy after all repetitions for each pattern [time delta index]
				double[] efficacyLog = new double[positionsCount[0]];

				if (progressMonitor != null) {
					progressMonitor.setMaximum(positionsCount[0]);
				}

				for (int timeDeltaIndex = 0; timeDeltaIndex < positionsCount[0]; timeDeltaIndex++) {
					if (progressMonitor != null) {
						progressMonitor.setProgress(timeDeltaIndex);
					}

					double position = (double) timeDeltaIndex / (positionsCount[0] - 1); // Position in variation dimension 1

					// Generate pre and post spike timing patterns for this position.
					for (int p = 0; p < 2; p++) {
						for (int si = 0; si < spikeCounts[p]; si++) { // If this spikes timing varies.
							int variationDim = variationDimForSpike[p][si];
							if (variationDim != 0) {
								currentSpikeTimings[p][si] = position * patterns[0][p][si] + (1 - position) * patterns[variationDim][p][si];
							}
						}
					}

					preConfig.spikeTimings = currentSpikeTimings[0];
					postConfig.spikeTimings = currentSpikeTimings[1];
					preConfig.fireChangeEvent();
					postConfig.fireChangeEvent();

					sim.reset();
					sim.run(simSteps);

					time[timeDeltaIndex] = position * timeDeltaInitial[0] + (1 - position) * timeDeltaFinal[0];
					efficacyLog[timeDeltaIndex] = synapse.getEfficacy(0);
				}

				results.setProperty("type", TYPE.STDP_1D);
				results.addResult("Efficacy", efficacyLog);
				results.addResult("Time delta", time);

				// We're testing spike patterns with gradually altered spike times over two dimensions.
			} else {
				// The change in synapse efficacy after all repetitions for each pattern
				// [time delta for var dim 1, time delta for var dim 2, synapse efficacy][result index]
				double[][] efficacyLog = new double[3][(positionsCount[0]) * (positionsCount[1])];

				if (progressMonitor != null) {
					progressMonitor.setMaximum(efficacyLog[0].length);
				}

				double[] position = new double[2]; // Position in variation dimensions 1 and 2
				for (int timeDeltaIndex1 = 0, resultIndex = 0; timeDeltaIndex1 < positionsCount[0]; timeDeltaIndex1++) {
					position[0] = (double) timeDeltaIndex1 / (positionsCount[0] - 1);

					for (int timeDeltaIndex2 = 0; timeDeltaIndex2 < positionsCount[1]; timeDeltaIndex2++, resultIndex++) {
						if (progressMonitor != null) {
							progressMonitor.setProgress(resultIndex);
						}

						position[1] = (double) timeDeltaIndex2 / (positionsCount[1] - 1);

						// Generate pre and post spike timing patterns for this position.
						for (int p = 0; p < 2; p++) {
							for (int si = 0; si < spikeCounts[p]; si++) { // If this spikes timing varies.
								int variationDim = variationDimForSpike[p][si];
								if (variationDim != 0) {
									currentSpikeTimings[p][si] = (1 - position[variationDim - 1]) * patterns[0][p][si] + position[variationDim - 1] * patterns[variationDim][p][si];
								}
							}
						}

						preConfig.spikeTimings = currentSpikeTimings[0];
						postConfig.spikeTimings = currentSpikeTimings[1];
						preConfig.fireChangeEvent();
						postConfig.fireChangeEvent();

						sim.reset();
						sim.run(simSteps);

						efficacyLog[0][resultIndex] = (1 - position[0]) * timeDeltaInitial[0] + position[0] * timeDeltaFinal[0];
						efficacyLog[1][resultIndex] = (1 - position[1]) * timeDeltaInitial[1] + position[1] * timeDeltaFinal[1];
						efficacyLog[2][resultIndex] = synapse.getEfficacy(0);
					}
				}

				results.setProperty("type", TYPE.STDP_2D);
				results.addResult("Time delta 1", "Time delta 2", "Efficacy", efficacyLog);
			}
		}

		return results;
	}

	/**
	 * Test a synapse on the specified spiking protocol or a series of spiking protocols derived from initial and final protocols by interpolation over one or
	 * two dimensions. The synapse is tested over all specified parameter configurations.
	 * 
	 * @param synapse The SynapseCollection containing the synapse to test (the first synapse is used). The first configuration (at index 0) in the collection
	 *            will be replaced with each configuration specified by the <em>configurations</em> argument.
	 * @param configurationLabels An array containing the labels for each parameter configuration to test on. These are used to label the data sets in the
	 *            returned TestResults.
	 * @param configurations An array containing the parameter configurations to test on.
	 * @param timeResolution The time resolution to use in the simulation, see {@link com.ojcoleman.bain.NeuralNetwork}
	 * @param period The period of the spike pattern in seconds.
	 * @param repetitions The number of times to apply the spike pattern.
	 * @param patterns Array containing spike patterns, in the form [initial, dim 1, dim 2][pre, post][spike number] = spike time. The [spike number] array
	 *            contains the times (s) of each spike, relative to the beginning of the pattern. See
	 *            {@link com.ojcoleman.bain.neuron.spiking.FixedProtocolNeuronCollection}.
	 * @param refSpikeIndexes Array specifying indexes of the two spikes to use as timing variation references for each variation dimension, in the form [dim 1,
	 *            dim 2][reference spike, relative spike] = spike index.
	 * @param refSpikePreOrPost Array specifying whether the timing variation reference spikes specified by refSpikeIndexes belong to the pre- or post-synaptic
	 *            neurons, in the form [dim 1, dim 2][base spike, relative spike] = Constants.PRE or Constants.POST.
	 * @param logSpikesAndStateVariables Whether to record pre- and post-synaptic spikes and any state variables exposed by the synapse model in the test
	 *            results.
	 * @param progressMonitor If not null, this will be updated to display the progress of the test.
	 * @return For a single spike protocol, a TestResults object with type {@link TYPE#STDP} consisting of series labelled "Time" and "Efficacy" and if
	 *         logSpikesAndStateVariables == true then also "Pre-synaptic spikes", "Post-synaptic spikes" and any state variables exposed by the synapse model.
	 *         For a protocol varied over one dimension, a TestResults object with type {@link TYPE#STDP_1D} consisting of series labelled "Time delta" and
	 *         "Efficacy". For a protocol varied over two dimensions, a TestResults object with type {@link TYPE#STDP_2D} consisting of series labelled
	 *         "Time delta 1", "Time delta 2" and "Efficacy".
	 */
	public static TestResults[] testPattern(SynapseCollection<? extends ComponentConfiguration> synapse, String[] configurationLabels, ComponentConfiguration[] configurations, int timeResolution, double period, int repetitions, double[][][] patterns, int[][] refSpikeIndexes, int[][] refSpikePreOrPost, boolean logSpikesAndStateVariables, ProgressMonitor progressMonitor) throws IllegalArgumentException {
		int configCount = configurationLabels.length;
		TestResults[] results = new TestResults[configCount];

		if (synapse.getConfigurationCount() == 0) {
			synapse.addConfiguration(configurations[0]);
		}

		ProgressMonitor progressMonitorSub = null;
		if (progressMonitor != null) {
			progressMonitor.setMinimum(0);
			progressMonitor.setMaximum(configCount);
			progressMonitor.setMillisToDecideToPopup(0);
			progressMonitorSub = new ProgressMonitor(null, null, "Performing test...", 0, 0);
		}

		for (int c = 0; c < configCount; c++) {
			if (progressMonitor != null) {
				progressMonitor.setProgress(c);
				progressMonitor.setNote("Testing " + configurationLabels[c]);
			}
			synapse.setConfiguration(0, configurations[c]);
			results[c] = testPattern(synapse, timeResolution, period, repetitions, patterns, refSpikeIndexes, refSpikePreOrPost, logSpikesAndStateVariables, progressMonitorSub);
			results[c].setProperty("label", configurationLabels[c]);
		}

		if (progressMonitorSub != null) {
			progressMonitorSub.close();
		}

		return results;
	}

	/**
	 * Plot the results produced by the testing methods in this class.
	 * 
	 * @param results The results of the test.
	 * @param timeResolution The time resolution which the simulation was run at.
	 * @param logSpikesAndStateVariables Whether to include pre- and post-synaptic spikes and any state variables exposed by the synapse model in the plot.
	 * @param showInFrame Whether to display the result in a frame.
	 */
	public static JFreeChart createChart(TestResults results, int timeResolution, boolean logSpikesAndStateVariables, boolean showInFrame, String title) {
		return createChart(new TestResults[] { results }, true, timeResolution, logSpikesAndStateVariables, showInFrame, title);
	}

	/**
	 * Plot the results produced by the testing methods in this class.
	 * 
	 * @param results The results of one or more tests. They must all have the same type {@link TYPE}. If more than one result is to be plotted then only
	 *            {@link TYPE#STDP} or {@link TYPE#STDP_1D} are supported, and it is assumed that they all have the same pre- and post-synaptic spike patterns.
	 * @param singlePlot If plotting more than one TestResult then whether to show the results on a single plot (true) or separate plots (false).
	 * @param timeResolution The time resolution which the simulation was run at.
	 * @param logSpikesAndStateVariables Whether to include pre- and post-synaptic spikes and any state variables exposed by the synapse model in the plot. This
	 *            is ignored if more than one result is to be plotted.
	 * @param showInFrame Whether to display the result in a frame.
	 */
	public static JFreeChart createChart(TestResults[] results, boolean singlePlot, int timeResolution, boolean logSpikesAndStateVariables, boolean showInFrame, String title) {
		JFreeChart resultsPlot = null;
		int resultsCount = results.length;

		// Make sure they're all the same type.
		for (int ri = 0; ri < results.length; ri++) {
			if (ri < resultsCount - 1 && results[ri].getProperty("type") != results[ri + 1].getProperty("type")) {
				throw new IllegalArgumentException("All results must have the same type.");
			}
			if (resultsCount > 1 && results[ri].getProperty("type") != TYPE.STDP && results[ri].getProperty("type") != TYPE.STDP_1D) {
				throw new IllegalArgumentException("Multiple results can only be plotted for types STDP or STDP_1D.");
			}
		}

		TYPE type = (TYPE) results[0].getProperty("type");

		XYLineAndShapeRenderer xyRenderer;
		XYToolTipGenerator tooltipGen = new StandardXYToolTipGenerator();

		if (type == TYPE.STDP) {
			CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(new NumberAxis("t (s)"));

			if (singlePlot) { // Plot all result sets together.
				DefaultXYDataset efficacyData = new DefaultXYDataset();
				for (TestResults result : results) {
					String efficacyLabel = (resultsCount == 1) ? "Efficacy" : "" + result.getProperty("label");
					efficacyData.addSeries(efficacyLabel, result.getResult("Time", "Efficacy"));
				}
				xyRenderer = new XYLineAndShapeRenderer(true, false);
				xyRenderer.setBaseToolTipGenerator(tooltipGen);
				combinedPlot.add(new XYPlot(efficacyData, null, new NumberAxis("Efficacy"), xyRenderer), 4);
			} else { // Plot each result set separately.
				for (TestResults result : results) {
					DefaultXYDataset efficacyData = new DefaultXYDataset();
					String efficacyLabel = (resultsCount == 1) ? "Efficacy" : "" + result.getProperty("label");
					efficacyData.addSeries(efficacyLabel, result.getResult("Time", "Efficacy"));
					xyRenderer = new XYLineAndShapeRenderer(true, false);
					xyRenderer.setBaseToolTipGenerator(tooltipGen);
					combinedPlot.add(new XYPlot(efficacyData, null, new NumberAxis("Efficacy"), xyRenderer), 4);
				}
			}

			// Don't plot trace data for multiple tests.
			if (resultsCount == 1 && logSpikesAndStateVariables) {
				DefaultXYDataset traceData = new DefaultXYDataset();
				for (String label : results[0].getResultLabels()) {
					if (!label.startsWith("Time") && !label.startsWith("Efficacy") && !label.equals("Pre-synaptic spikes") && !label.equals("Post-synaptic spikes")) {
						traceData.addSeries(label, results[0].getResult("Time", label));
					}
				}
				xyRenderer = new XYLineAndShapeRenderer(true, false);
				xyRenderer.setBaseToolTipGenerator(tooltipGen);
				combinedPlot.add(new XYPlot(traceData, null, new NumberAxis("State"), xyRenderer), 3);

				DefaultXYDataset spikeData = new DefaultXYDataset();
				spikeData.addSeries("Pre-synaptic spikes", results[0].getResult("Time", "Pre-synaptic spikes"));
				spikeData.addSeries("Post-synaptic spikes", results[0].getResult("Time", "Post-synaptic spikes"));
				xyRenderer = new XYLineAndShapeRenderer(true, false);
				xyRenderer.setBaseToolTipGenerator(tooltipGen);
				combinedPlot.add(new XYPlot(spikeData, null, new NumberAxis("Pre/post potential"), xyRenderer), 3);
			}

			resultsPlot = new JFreeChart(title, null, combinedPlot, true);
			resultsPlot.setBackgroundPaint(Color.WHITE);
			resultsPlot.getPlot().setBackgroundPaint(Color.WHITE);
			((XYPlot) resultsPlot.getPlot()).setRangeGridlinePaint(Color.LIGHT_GRAY);
			((XYPlot) resultsPlot.getPlot()).setDomainGridlinePaint(Color.LIGHT_GRAY);

		}

		else if (type == TYPE.STDP_1D) {
			DecimalFormat timeFormatter = new DecimalFormat();
			timeFormatter.setMultiplier(1000);
			NumberAxis domainAxis = new NumberAxis("\u0394t (ms)");
			domainAxis.setNumberFormatOverride(timeFormatter);
			CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(domainAxis);

			if (singlePlot) { // Plot all result sets together.
				DefaultXYDataset efficacyData = new DefaultXYDataset();
				for (TestResults result : results) {
					String efficacyLabel = (resultsCount == 1) ? "Efficacy" : "" + result.getProperty("label");
					efficacyData.addSeries(efficacyLabel, result.getResult("Time delta", "Efficacy"));
				}
				xyRenderer = new XYLineAndShapeRenderer(true, false);
				xyRenderer.setBaseToolTipGenerator(tooltipGen);
				combinedPlot.add(new XYPlot(efficacyData, null, new NumberAxis("Efficacy"), xyRenderer), 4);
			} else { // Plot each result set separately.
				for (TestResults result : results) {
					DefaultXYDataset efficacyData = new DefaultXYDataset();
					String efficacyLabel = (resultsCount == 1) ? "Efficacy" : "" + result.getProperty("label");
					efficacyData.addSeries(efficacyLabel, result.getResult("Time delta", "Efficacy"));
					xyRenderer = new XYLineAndShapeRenderer(true, false);
					xyRenderer.setBaseToolTipGenerator(tooltipGen);
					combinedPlot.add(new XYPlot(efficacyData, null, new NumberAxis("Efficacy"), xyRenderer), 4);
				}
			}

			resultsPlot = new JFreeChart(title, null, combinedPlot, true);
			resultsPlot.setBackgroundPaint(Color.WHITE);
			resultsPlot.getPlot().setBackgroundPaint(Color.WHITE);
			((XYPlot) resultsPlot.getPlot()).setRangeGridlinePaint(Color.LIGHT_GRAY);
			((XYPlot) resultsPlot.getPlot()).setDomainGridlinePaint(Color.LIGHT_GRAY);
		}

		else if (type == TYPE.STDP_2D) {
			double[][] data = results[0].getResult("Time delta 1", "Time delta 2", "Efficacy");

			DefaultXYZDataset plotData = new DefaultXYZDataset();
			plotData.addSeries("Efficacy", data);

			// Set up paint scale, and convert domain axes from seconds to
			// milliseconds (XYBlockRenderer won't deal with fractional values
			// in the domain axes)
			double min = Double.MAX_VALUE, max = -min;
			double[] efficacy = data[2];
			for (int i = 0; i < data[0].length; i++) {
				if (efficacy[i] < min)
					min = efficacy[i];
				if (efficacy[i] > max)
					max = efficacy[i];

				data[0][i] = Math.round(data[0][i] * 1000);
				data[1][i] = Math.round(data[1][i] * 1000);
			}

			XYBlockRenderer renderer = new XYBlockRenderer();

			double range = Math.max(Math.abs(min), Math.abs(max));
			double rangeBase = 0;
			if (min < 0)
				min = -range;
			if (max > 0)
				max = range;
			// If the value range does not cross the zero point, don't use a zero-based range.
			if ((min > 0) || (max < 0)) {
				range = Math.abs(max - min);
				rangeBase = Math.min(Math.abs(min), Math.abs(max));
			}
			if (min >= max) {
				max = min + Double.MIN_VALUE * 10;
			}

			LookupPaintScale scale = new LookupPaintScale(min, max, Color.WHITE);
			if (min < 0) {
				for (int ci = 0; ci <= 255; ci++) {
					double v = -(ci / 255.0) * range - rangeBase;
					scale.add(v, new Color(0, ci, ci));
				}
			}
			if (max > 0) {
				for (int ci = 0; ci <= 255; ci++) {
					double v = (ci / 255.0) * range + rangeBase;
					scale.add(v, new Color(ci, ci, 0));
				}
			}
			renderer.setPaintScale(scale);
			renderer.setSeriesToolTipGenerator(0, new StandardXYZToolTipGenerator());
			int displayResolution = ((Integer) results[0].getProperty("display time resolution")).intValue();
			renderer.setBlockWidth(1000.0 / displayResolution);
			renderer.setBlockHeight(1000.0 / displayResolution);

			NumberAxis xAxis = new NumberAxis("\u0394t1 (ms)");
			NumberAxis yAxis = new NumberAxis("\u0394t2 (ms)");

			XYPlot plot = new XYPlot(plotData, xAxis, yAxis, renderer);
			plot.setDomainGridlinesVisible(false);

			resultsPlot = new JFreeChart(title, plot);
			resultsPlot.removeLegend();
			NumberAxis valueAxis = new NumberAxis();
			valueAxis.setLowerBound(scale.getLowerBound());
			valueAxis.setUpperBound(scale.getUpperBound());
			PaintScaleLegend legend = new PaintScaleLegend(scale, valueAxis);
			legend.setPosition(RectangleEdge.RIGHT);
			legend.setMargin(5, 5, 5, 5);
			resultsPlot.addSubtitle(legend);
		}

		if (showInFrame) {
			JFrame plotFrame = new JFrame(title);
			plotFrame.add(new ChartPanel(resultsPlot));
			plotFrame.setExtendedState(plotFrame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
			plotFrame.pack();
			plotFrame.setVisible(true);
		}

		return resultsPlot;
	}
}