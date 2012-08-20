package ojc.bain.test;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.Arrays;

import javax.swing.JFrame;
import javax.swing.JProgressBar;

import ojc.bain.Simulation;
import ojc.bain.base.ComponentConfiguration;
import ojc.bain.base.SynapseCollection;
import ojc.bain.misc.*;
import ojc.bain.neuron.*;
import ojc.bain.synapse.*;

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

/**
 * Class to ojc.bain.test the behaviour of a model on a specific spiking protocol or spiking protocols over some timing adjustment.
 * 
 * @author Oliver J. Coleman
 */
public class SynapseTest {
	/**
	 * The type of test results.
	 */
	public static enum TYPE {
		STDP, STDP_1D, STDP_2D;
	};

	/*
	 * @param synapse The SynapseCollection containing the synapse to test (the first synapse is used).
	 * 
	 * @param timeResolution The time resolution to use in the simulation, see {@link ojc.bain.Simulation}
	 * 
	 * @param period The period of the spike pattern in seconds.
	 * 
	 * @param repetitions The number of times to apply the spike pattern.
	 * 
	 * @param patterns Array containing spike patterns, in the form [initial, dim 1, dim 2][pre, post][spike number] = spike time. The [spike number] array
	 * contains the times (s) of each spike, relative to the beginning of the pattern. See {@link ojc.bain.neuron.FixedProtocolNeuronCollection}.
	 * 
	 * @param refSpikeIndexes Array specifying indexes of the two spikes to use as timing variation references for each variation dimension, in the form [dim 1,
	 * dim 2][reference spike, relative spike] = spike index.
	 * 
	 * @param refSpikePreOrPost Array specifying whether the timing variation reference spikes specified by refSpikeIndexes belong to the pre- or post-synaptic
	 * neurons, in the form [dim 1, dim 2][base spike, relative spike] = Constants.PRE or Constants.POST.
	 * 
	 * @param logSpikesAndStateVariables Whether or not to include logs of spikes and state variables from the synape in the test results.
	 * 
	 * @param progressBar If not null, this will be updated to display the progress of the test.
	 */

	public static TestResults testPattern(SynapseCollection<? extends ComponentConfiguration> synapse, int timeResolution, double period, int repetitions, double[][][] patterns, int[][] refSpikeIndexes, int[][] refSpikePreOrPost, boolean logSpikesAndStateVariables, JProgressBar progressBar) throws IllegalArgumentException {
		int variationDimsCount = patterns.length - 1; // Number of dimensions over which spike timingpatterns vary.
		if (variationDimsCount > 2) {
			throw new IllegalArgumentException("The number of variation dimensions may not exceed 2 (patterns.length must be <= 3)");
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

		Simulation sim = new Simulation(timeResolution, neurons, synapse);
		Simulation.setSingleton(sim);

		int simSteps = (int) Math.round(period * repetitions * timeResolution);

		int displayTimeResolution = Math.min(1000, timeResolution);

		results.setProperty("simulation time resolution", timeResolution);
		results.setProperty("display time resolution", displayTimeResolution);

		// long startTime = System.currentTimeMillis();

		// If we're just testing a single spike pattern. // Handle separately as logging is quite different from testing spike
		// patterns with gradually altered spike times.
		if (variationDimsCount == 0) {
			results = singleTest(sim, simSteps, logSpikesAndStateVariables);
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

				if (progressBar != null) {
					progressBar.setMaximum(positionsCount[0]);
				}

				for (int timeDeltaIndex = 0; timeDeltaIndex < positionsCount[0]; timeDeltaIndex++) {
					if (progressBar != null) {
						progressBar.setValue(timeDeltaIndex);
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

				if (progressBar != null) {
					progressBar.setMaximum(efficacyLog[0].length);
				}

				double[] position = new double[2]; // Position in variation dimensions 1 and 2
				for (int timeDeltaIndex1 = 0, resultIndex = 0; timeDeltaIndex1 < positionsCount[0]; timeDeltaIndex1++) {
					position[0] = (double) timeDeltaIndex1 / (positionsCount[0] - 1);

					for (int timeDeltaIndex2 = 0; timeDeltaIndex2 < positionsCount[1]; timeDeltaIndex2++, resultIndex++) {
						if (progressBar != null) {
							progressBar.setValue(resultIndex);
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

		// long finishTime = System.currentTimeMillis();
		// System.out.println("Took " + ((finishTime - startTime) / 1000f) + "s");

		return results;
	}

	/**
	 * Test the behaviour of a model.
	 * 
	 * @param sim A Simulation containing two neurons and a single to be tested. The neurons at index 0 and 1 should be the pre- and post-synaptic neurons
	 *            respectively, and will typically produce a fixed firing pattern.
	 * @param simSteps the number of simulation steps to run the simulation for.
	 * @param logSpikesAndStateVariables Whether to log pre- and post-synaptic spikes, and any state variables for the .
	 */
	public static TestResults singleTest(Simulation sim, int simSteps, boolean logSpikesAndStateVariables) {
		if (sim.getNeurons().getSize() != 2 || sim.getSynapses().getSize() != 1) {
			throw new IllegalArgumentException("The simulation must contain at least 2 neurons and 1 .");
		}

		int displayTimeResolution = Math.min(1000, sim.getTimeResolution());

		int logStepCount = sim.getTimeResolution() / displayTimeResolution;
		int logSize = simSteps / logStepCount;

		double[] timeLog = new double[logSize];
		double[] efficacyLog = new double[logSize];
		double[][] prePostLogs = null, traceLogs = null;
		double[] stateVars;
		if (logSpikesAndStateVariables) {
			prePostLogs = new double[2][logSize];
			traceLogs = new double[sim.getSynapses().getStateVariableNames().length][logSize];
		}

		int logIndex = 0;
		for (int step = 0; step < simSteps; step++) {
			double time = sim.getTime();
			sim.step();

			if (step % logStepCount == 0) {
				timeLog[logIndex] = time;
				efficacyLog[logIndex] = sim.getSynapses().getEfficacy(0);
				// If we're only testing a few repetitions, include some extra
				// data.
				if (logSpikesAndStateVariables) {
					prePostLogs[0][logIndex] = sim.getNeurons().getOutput(0);
					prePostLogs[1][logIndex] = sim.getNeurons().getOutput(1);
					stateVars = sim.getSynapses().getStateVariableValues(0);
					for (int v = 0; v < stateVars.length; v++) {
						traceLogs[v][logIndex] = stateVars[v];
					}
				}
				logIndex++;
			}
		}

		TestResults results = new TestResults();
		results.setProperty("type", TYPE.STDP);
		results.addResult("Efficacy", efficacyLog);
		results.addResult("Time", timeLog);
		if (logSpikesAndStateVariables) {
			results.addResult("Pre-synaptic spikes", prePostLogs[0]);
			results.addResult("Post-synaptic spikes", prePostLogs[1]);
			String[] stateVariableNames = sim.getSynapses().getStateVariableNames();
			for (int v = 0; v < stateVariableNames.length; v++) {
				results.addResult(stateVariableNames[v], traceLogs[v]);
			}
		}

		return results;
	}

	/**
	 * Plot the results produced by the testing methods in this class.
	 * 
	 * @param results The results of the ojc.bain.test.
	 * @param timeResolution The time resolution which the simulation was run at.
	 * @param logSpikesAndStateVariables Whether to log pre- and post-synaptic spikes, and any state variables for the .
	 * @param showInFrame Whether to display the result in a frame.
	 */
	public static JFreeChart createChart(TestResults results, int timeResolution, boolean logSpikesAndStateVariables, boolean showInFrame) {
		JFreeChart resultsPlot = null;

		if (results.getProperty("type") == TYPE.STDP) {
			CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(new NumberAxis("t (s)"));
			XYToolTipGenerator tooltipGen = new StandardXYToolTipGenerator();
			XYLineAndShapeRenderer xyRenderer = new XYLineAndShapeRenderer(true, false);
			xyRenderer.setBaseToolTipGenerator(tooltipGen);

			DefaultXYDataset efficacyData = new DefaultXYDataset();
			efficacyData.addSeries("Efficacy", results.getResult("Time", "Efficacy"));

			combinedPlot.add(new XYPlot(efficacyData, null, new NumberAxis("Efficacy"), xyRenderer), 4);
			if (logSpikesAndStateVariables) {
				DefaultXYDataset spikeData = new DefaultXYDataset();
				spikeData.addSeries("Pre-synaptic spikes", results.getResult("Time", "Pre-synaptic spikes"));
				spikeData.addSeries("Post-synaptic spikes", results.getResult("Time", "Post-synaptic spikes"));
				XYBarRenderer xybr = new XYBarRenderer();
				xybr.setShadowVisible(false);
				combinedPlot.add(new XYPlot(new XYBarDataset(spikeData, 1.0 / timeResolution), null, new NumberAxis("Spikes"), xybr), 1);

				DefaultXYDataset traceData = new DefaultXYDataset();
				for (String label : results.getResultLabels()) {
					if (!label.startsWith("Time") && !label.startsWith("Efficacy") && !label.equals("Pre-synaptic spikes") && !label.equals("Post-synaptic spikes")) {
						traceData.addSeries(label, results.getResult("Time", label));
					}
				}
				combinedPlot.add(new XYPlot(traceData, null, new NumberAxis("Traces"), xyRenderer), 3);
			}
			resultsPlot = new JFreeChart("Synapse ojc.bain.test", null, combinedPlot, true); // ChartFactory.createXYLineChart("",
			// "t (s)",
			// "",
			// plotData,
			// PlotOrientation.VERTICAL,
			// true,
			// true,
			// false);
			resultsPlot.setBackgroundPaint(Color.WHITE);
		}

		else if (results.getProperty("type") == TYPE.STDP_1D) {
			DefaultXYDataset plotData = new DefaultXYDataset();
			plotData.addSeries("Efficacy", results.getResult("Time delta", "Efficacy"));
			resultsPlot = ChartFactory.createXYLineChart("", "\u0394t (ms)", "", plotData, PlotOrientation.VERTICAL, true, true, false);
			DecimalFormat timeFormatter = new DecimalFormat();
			timeFormatter.setMultiplier(1000);
			((NumberAxis) resultsPlot.getXYPlot().getDomainAxis()).setNumberFormatOverride(timeFormatter);
		}

		else if (results.getProperty("type") == TYPE.STDP_2D) {
			double[][] data = results.getResult("Time delta 1", "Time delta 2", "Efficacy");

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
			int displayResolution = (int) results.getProperty("display time resolution");
			renderer.setBlockWidth(1000.0 / displayResolution);
			renderer.setBlockHeight(1000.0 / displayResolution);

			NumberAxis xAxis = new NumberAxis("\u0394t1 (ms)");
			NumberAxis yAxis = new NumberAxis("\u0394t2 (ms)");

			XYPlot plot = new XYPlot(plotData, xAxis, yAxis, renderer);
			plot.setDomainGridlinesVisible(false);

			resultsPlot = new JFreeChart("", plot);
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
			JFrame plotFrame = new JFrame();
			plotFrame.add(new ChartPanel(resultsPlot));
			plotFrame.setExtendedState(plotFrame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
			plotFrame.pack();
			plotFrame.setVisible(true);
		}

		return resultsPlot;
	}
}