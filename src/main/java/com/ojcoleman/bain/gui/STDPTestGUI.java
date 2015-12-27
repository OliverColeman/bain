package com.ojcoleman.bain.gui;

import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;


import org.jdesktop.swingx.*;
import org.jdesktop.swingx.multislider.*;
import org.jfree.chart.*;

import com.ojcoleman.bain.base.*;
import com.ojcoleman.bain.misc.*;
import com.ojcoleman.bain.neuron.*;
import com.ojcoleman.bain.synapse.*;

/**
 * GUI for experimenting with synaptic plasticity models.
 * 
 * @author Oliver J. Coleman
 */
public class STDPTestGUI extends JPanel {
	private static final long serialVersionUID = 1L;
	static final int margin = 8;

	final STDPTestGUI gui;
	final SpikeProtocolSettingsPanel spikeSettings;
	final SynapseSettingsPanel synapseSettings;

	public STDPTestGUI() {
		setPreferredSize(new Dimension(1000, 650));

		gui = this;

		spikeSettings = new SpikeProtocolSettingsPanel(this);
		synapseSettings = new SynapseSettingsPanel(this);

		final JSpinner timeResolutionSpinner = new JSpinner(new SpinnerNumberModel(1000, 0, 100000, 1));
		timeResolutionSpinner.setToolTipText("<html>This is the number of steps per second of simulation time. The more steps, the more accurate the simulation.<br />A typical value is 1000, corresponding to a step duration of 1 millisecond.</html>");

		final JButton testSpecifiedButton = new JButton("<html>Test on specified synapse settings</html>");
		testSpecifiedButton.setToolTipText("<html>Click here to run the test using the current settings.<br />This can take several minutes or more, depending on the time resolution, number of variation dimensions, and total time differential in each variation dimension.</html>");

		final JButton testPresetsSingleButton = new JButton("<html>Test on all synapse presets,<br />single plot</html>");
		testPresetsSingleButton.setToolTipText("<html>Click here to run the test on each preset available for the selected synapse.<br />This can take several minutes or more, depending on the time resolution, number of variation dimensions, and total time differential in each variation dimension.</html>");

		final JButton testPresetsSeparateButton = new JButton("<html>Test on all synapse presets,<br />separate plots</html>");
		testPresetsSeparateButton.setToolTipText("<html>Click here to run the test on each preset available for the selected synapse.<br />This can take several minutes or more, depending on the time resolution, number of variation dimensions, and total time differential in each variation dimension.</html>");

		ActionListener testButtonActionListenter = new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				final SynapseCollection synapse = synapseSettings.getSynapse();
				final SpikeProtocolSettings settings = spikeSettings.getSpikeProtocolSettings();

				// If something is wrong with the selected synapse or settings.
				if (synapse == null || settings == null) {
					JOptionPane.showMessageDialog(gui, "Can not perform tests, the synapse or settings could not be loaded", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}

				testSpecifiedButton.setEnabled(false);
				testPresetsSingleButton.setEnabled(false);
				testPresetsSeparateButton.setEnabled(false);

				final int timeResolution = ((SpinnerNumberModel) timeResolutionSpinner.getModel()).getNumber().intValue();

				final ProgressMonitor progressMonitor = new ProgressMonitor(gui, null, "Performing test...", 0, 0);

				SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
					@Override
					protected Void doInBackground() {
						if (e.getSource() == testSpecifiedButton) {
							boolean logSpikesAndStateVariables = settings.repetitions <= 25;
							TestResults results = SynapseTest.testPattern(synapse, timeResolution, settings.period, settings.repetitions, settings.patterns, settings.refSpikeIndexes, settings.refSpikePreOrPost, logSpikesAndStateVariables, progressMonitor);

							String title = synapse.getClass().getSimpleName() + " (" + synapse.getComponentConfiguration(0).name + ") - " + settings.name;

							JFreeChart resultsPlot = SynapseTest.createChart(results, timeResolution, logSpikesAndStateVariables, true, title);
						} else {
							ComponentConfiguration config = synapse.getConfigSingleton();
							TestResults[] results = SynapseTest.testPattern(synapse, config.getPresetNames(), config.getPresets(), timeResolution, settings.period, settings.repetitions, settings.patterns, settings.refSpikeIndexes, settings.refSpikePreOrPost, false, progressMonitor);

							// for (int r = 0; r < results.length; r++) {
							// results[r].setProperty("Title", synapse.getClass().getSimpleName() + " (" + config.getPresetNames()[r] + ") - " + settings.name);
							// }

							String title = synapse.getClass().getSimpleName() + " - " + settings.name;

							boolean singlePlot = e.getSource() == testPresetsSingleButton;
							JFreeChart resultsPlot = SynapseTest.createChart(results, singlePlot, timeResolution, false, true, title);
						}
						return null;
					}

					protected void done() {
						progressMonitor.close();
						// Deal with exception thrown in doInBackground()
						// See http://stackoverflow.com/questions/6523623/gracefull-exception-handling-in-swing-worker
						try {
							get();
						} catch (Exception e) {
							e.getCause().printStackTrace();
							String msg = String.format("Unexpected problem: %s", e.getCause().toString());
							JOptionPane.showMessageDialog(gui, msg, "Error", JOptionPane.ERROR_MESSAGE);
						}
						testSpecifiedButton.setEnabled(true);
						testPresetsSingleButton.setEnabled(true);
						testPresetsSeparateButton.setEnabled(true);
					}
				};
				worker.execute();
			}
		};

		testSpecifiedButton.addActionListener(testButtonActionListenter);
		testPresetsSingleButton.addActionListener(testButtonActionListenter);
		testPresetsSeparateButton.addActionListener(testButtonActionListenter);

		JTabbedPane tabPane = new JTabbedPane();
		tabPane.addTab("Spiking protocol", spikeSettings);
		tabPane.addTab("Synapse", synapseSettings);

		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.weightx = 4;
		gbc.weighty = 0.1;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.BOTH;
		add(testSpecifiedButton, gbc);
		gbc.gridx = 1;
		add(testPresetsSingleButton, gbc);
		gbc.gridx = 2;
		add(testPresetsSeparateButton, gbc);
		gbc.gridx = 3;
		gbc.weightx = 1;
		add(createLabeledComponent("Resolution:", timeResolutionSpinner), gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weighty = 1;
		gbc.gridwidth = 4;
		add(tabPane, gbc);

		// setVisible(true);
	}

	private class SpikeProtocolSettingsPanel extends JPanel {
		private static final long serialVersionUID = 1L;

		SpikeTimingSetter[][] spikeTimingSetters;
		SpikeTimingSetterPair[] spikeTimingSetterPairs;
		JSpinner variationDimsSpinner, preSpikeCountSpinner, postSpikeCountSpinner, patternFreqSpinner, patternRepetitionsSpinner;
		JComboBox<String> presetSelector;

		public SpikeProtocolSettingsPanel(final STDPTestGUI gui) {
			SpikeProtocolSettings initSettings = spikeProtocolSettingsPresets[0];
			final int maxSpikePatternVariationDimensions = 2;

			JPanel spikeTimingSetterPanel = new JPanel(new GridBagLayout());
			final SpikeTimingSetterPair[] spikeTimingSetterPairs = new SpikeTimingSetterPair[maxSpikePatternVariationDimensions + 1];
			final SpikeTimingSetter[][] spikeTimingSetters = new SpikeTimingSetter[maxSpikePatternVariationDimensions + 1][2]; // [dimension][pre,
																																// post]

			final JSpinner variationDimsSpinner = new JSpinner(new SpinnerNumberModel(initSettings.variationDimsCount, 0, maxSpikePatternVariationDimensions, 1));
			variationDimsSpinner.setToolTipText("<html>The spiking protocol can be varied over none, one or two dimensions. For one and two variation dimensions, spiking protocols are generated by interpolating between the initial and final protocols.<br />" + "For none, the plot will show how the synapse efficacy changes over time as the protocol is presented, and will also plot the state variables and spikes if <em># protocol repetitions</em> is less than 26 and a single test is being performed by clicking on the <em>Test on specified synapse settings</em> button.<br />" + "For one and two variation dimensions, the plot will show the final efficacy after the spiking protocol has been presented <em># protocol repetitions</em> times, for each variation of the protocol.</html>");
			variationDimsSpinner.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					int dimCount = ((SpinnerNumberModel) variationDimsSpinner.getModel()).getNumber().intValue();
					for (int d = 1; d < maxSpikePatternVariationDimensions + 1; d++) {
						spikeTimingSetterPairs[d].setVisible(dimCount >= d);
					}
				}
			});

			final JSpinner preSpikeCountSpinner = new JSpinner(new SpinnerNumberModel(initSettings.spikeCounts[0], 0, 10, 1));
			preSpikeCountSpinner.setToolTipText("The number of pre-synaptic spikes in the protocol.");
			preSpikeCountSpinner.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					int spikeCount = ((SpinnerNumberModel) ((JSpinner) e.getSource()).getModel()).getNumber().intValue();
					for (int d = 0; d < spikeTimingSetters.length; d++) {
						spikeTimingSetters[d][0].setSpikeCount(spikeCount);
					}
				}
			});
			final JSpinner postSpikeCountSpinner = new JSpinner(new SpinnerNumberModel(initSettings.spikeCounts[1], 0, 10, 1));
			postSpikeCountSpinner.setToolTipText("The number of post-synaptic spikes in the protocol.");
			postSpikeCountSpinner.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					int spikeCount = ((SpinnerNumberModel) ((JSpinner) e.getSource()).getModel()).getNumber().intValue();
					for (int d = 0; d < spikeTimingSetters.length; d++) {
						spikeTimingSetters[d][1].setSpikeCount(spikeCount);
					}
				}
			});
			final JSpinner patternFreqSpinner = new JSpinner(new SpinnerNumberModel(1, 0.1, 100, 0.1));
			patternFreqSpinner.setToolTipText("The frequency with which the protocol is presented. The inverse of this is the duration of the protocol in seconds.");
			patternFreqSpinner.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					double period = 1.0 / ((SpinnerNumberModel) patternFreqSpinner.getModel()).getNumber().doubleValue();
					for (int d = 0; d < spikeTimingSetters.length; d++) {
						for (int p = 0; p < spikeTimingSetters[d].length; p++) {
							spikeTimingSetters[d][p].setPeriod(period);
						}
					}
				}
			});
			final JSpinner patternRepetitionsSpinner = new JSpinner(new SpinnerNumberModel(60, 1, 1000, 1));
			patternRepetitionsSpinner.setToolTipText("The number of times to present the spiking protocol.");

			JPanel fieldsPanel = new JPanel();
			fieldsPanel.setPreferredSize(new Dimension(1000, 100));
			fieldsPanel.add(createLabeledComponent("# protocol variation dimensions:", variationDimsSpinner));
			fieldsPanel.add(createLabeledComponent("# pre-synaptic spikes:", preSpikeCountSpinner));
			fieldsPanel.add(createLabeledComponent("# post-synaptic spikes:", postSpikeCountSpinner));
			fieldsPanel.add(createLabeledComponent("Protocol presentation frequency (Hz):", patternFreqSpinner));
			fieldsPanel.add(createLabeledComponent("# protocol repetitions:", patternRepetitionsSpinner));

			final JComboBox<String> presetSelector = new JComboBox<String>(spikeProtocolSettingsPresetNames);
			presetSelector.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					SpikeProtocolSettings settings = spikeProtocolSettingsPresets[presetSelector.getSelectedIndex()];

					variationDimsSpinner.setValue(settings.variationDimsCount);
					preSpikeCountSpinner.setValue(settings.spikeCounts[0]);
					postSpikeCountSpinner.setValue(settings.spikeCounts[1]);
					patternFreqSpinner.setValue(1.0 / settings.period);
					patternRepetitionsSpinner.setValue(settings.repetitions);
					for (int d = 0; d <= settings.variationDimsCount; d++) {
						for (int p = 0; p < 2; p++) {
							for (int s = 0; s < settings.spikeCounts[p]; s++) {
								spikeTimingSetterPairs[d].setSpikeTime(p, s, (int) Math.round(settings.patterns[d][p][s] * 1000));
							}
							if (d > 0) {
								spikeTimingSetterPairs[d].setBaseRef(settings.refSpikePreOrPost[d - 1][0], settings.refSpikeIndexes[d - 1][0]);
								spikeTimingSetterPairs[d].setRelativeRef(settings.refSpikePreOrPost[d - 1][1], settings.refSpikeIndexes[d - 1][1]);
							}
						}

					}
				}
			});

			GridBagConstraints gbc = new GridBagConstraints();
			gbc.weightx = 1;
			gbc.weighty = 1;
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.fill = GridBagConstraints.HORIZONTAL;

			add(createLabeledComponent("Spike protocol preset: ", presetSelector));

			add(fieldsPanel);

			for (int d = 0; d < maxSpikePatternVariationDimensions + 1; d++) {
				spikeTimingSetterPairs[d] = new SpikeTimingSetterPair(d > 0);
				spikeTimingSetterPairs[d].setBorder(createBorder(d == 0 ? "Initial spike protocol" : "Final spike protocol for timing variation dimension " + d));
				for (int p = 0; p < 2; p++) {
					spikeTimingSetters[d][p] = new SpikeTimingSetter(p == 0 ? "Pre" : "Post", p == 0 ? initSettings.spikeCounts[0] : initSettings.spikeCounts[1], initSettings.period, spikeTimingSetterPairs[d], p);
					spikeTimingSetterPairs[d].add(spikeTimingSetters[d][p]);
				}
				gbc.gridx = d % 2;
				gbc.gridy = d / 2;
				spikeTimingSetterPairs[d].setVisible(d <= initSettings.variationDimsCount);
				spikeTimingSetterPanel.add(spikeTimingSetterPairs[d], gbc);
			}

			spikeTimingSetterPanel.setPreferredSize(new Dimension(1000, 400));
			add(spikeTimingSetterPanel);

			presetSelector.setSelectedIndex(0);

			this.presetSelector = presetSelector;
			this.spikeTimingSetters = spikeTimingSetters;
			this.spikeTimingSetterPairs = spikeTimingSetterPairs;
			this.variationDimsSpinner = variationDimsSpinner;
			this.preSpikeCountSpinner = preSpikeCountSpinner;
			this.postSpikeCountSpinner = postSpikeCountSpinner;
			this.patternFreqSpinner = patternFreqSpinner;
			this.patternRepetitionsSpinner = patternRepetitionsSpinner;
		}

		public SpikeProtocolSettings getSpikeProtocolSettings() {
			SpikeProtocolSettings settings = new SpikeProtocolSettings();
			settings.name = presetSelector.getSelectedItem().toString();
			settings.variationDimsCount = ((SpinnerNumberModel) variationDimsSpinner.getModel()).getNumber().intValue();
			settings.period = 1.0 / ((SpinnerNumberModel) patternFreqSpinner.getModel()).getNumber().doubleValue();
			settings.repetitions = ((SpinnerNumberModel) patternRepetitionsSpinner.getModel()).getNumber().intValue();
			settings.spikeCounts = new int[2];
			settings.spikeCounts[0] = ((SpinnerNumberModel) preSpikeCountSpinner.getModel()).getNumber().intValue();
			settings.spikeCounts[1] = ((SpinnerNumberModel) postSpikeCountSpinner.getModel()).getNumber().intValue();
			// [initial, dim 1, dim 2][pre, post][spike index]
			settings.patterns = new double[settings.variationDimsCount + 1][2][];
			// [dim 1, dim 2][pre, post]
			settings.refSpikeIndexes = new int[settings.variationDimsCount][2];
			// [dim 1, dim 2][pre, post]
			settings.refSpikePreOrPost = new int[settings.variationDimsCount][2];

			// Used for error checking
			// [pre, post][spike index]
			int[][] variationDimForSpike = new int[2][Math.max(settings.spikeCounts[0], settings.spikeCounts[1])];

			for (int d = 0; d < settings.variationDimsCount + 1; d++) {
				for (int p = 0; p < 2; p++) {
					settings.patterns[d][p] = spikeTimingSetters[d][p].getSpikeTimings();
				}
				if (d > 0) {
					settings.refSpikeIndexes[d - 1][0] = spikeTimingSetterPairs[d].getBaseRefSpike();
					settings.refSpikeIndexes[d - 1][1] = spikeTimingSetterPairs[d].getRelativeRefSpike();
					settings.refSpikePreOrPost[d - 1][0] = spikeTimingSetterPairs[d].getBaseRefPreOrPost();
					settings.refSpikePreOrPost[d - 1][1] = spikeTimingSetterPairs[d].getRelativeRefPreOrPost();

					for (int p = 0; p < 2; p++) {
						if (settings.refSpikeIndexes[d - 1][p] == -1) {
							JOptionPane.showMessageDialog(null, "Please select a base and/or relative spike (left or right click on relevant spike, respectively) in variation dimension " + d + " to specify relevant time delta.", "Error", JOptionPane.ERROR_MESSAGE);
							return null;
						}

						// Ensure that a spikes timing only varies over at most one dimension.
						for (int si = 0; si < settings.spikeCounts[p]; si++) {
							// If the spikes time in variation dimension d is
							// different to the initial spike time.
							if (settings.patterns[0][p][si] != settings.patterns[d][p][si]) {
								// If it also differs in another dimension.
								if (variationDimForSpike[p][si] != 0) {
									JOptionPane.showMessageDialog(null, "A spikes timing may vary over at most one variation dimension. " + (p == 0 ? "Pre" : "Post") + "-synaptic spike " + (si + 1) + " varies over two.", "Error", JOptionPane.ERROR_MESSAGE);
								}
								variationDimForSpike[p][si] = d;
							}
						}
					}

					// double baseRefSpikeTimeInitial = settings.patterns[0][settings.refSpikePreOrPost[d - 1][0]][settings.refSpikeIndexes[d - 1][0]];
					// double baseRefSpikeTimeFinal = settings.patterns[d][settings.refSpikePreOrPost[d - 1][0]][settings.refSpikeIndexes[d - 1][0]];
					// if (baseRefSpikeTimeInitial != baseRefSpikeTimeFinal) {
					// JOptionPane.showMessageDialog(null,
					// "It is recommended that the initial and final base spike times be the same (and only the relative spike time differs).", "Warning",
					// JOptionPane.WARNING_MESSAGE);
					// }
				}
			}
			return settings;
		}
	}

	protected static String[] spikeProtocolSettingsPresetNames = new String[] { "pre-post, 2Hz, 20 reps, 10ms", "pre-post, 1Hz, 60 reps, -50ms to 50ms", "post-pre-post, 1Hz, 60 reps, 0ms to 50ms" };
	protected static SpikeProtocolSettings[] spikeProtocolSettingsPresets = new SpikeProtocolSettings[] { new SpikeProtocolSettings(spikeProtocolSettingsPresetNames[0], 0, 0.5, 20, new int[] { 1, 1 }, new double[][][] { { { 0.0 }, { 0.01 } } }, new int[][] { {} }, new int[][] { {} }), new SpikeProtocolSettings(spikeProtocolSettingsPresetNames[1], 1, 1, 60, new int[] { 1, 1 }, new double[][][] { { { 0.05 }, { 0.0 } }, { { 0.05 }, { 0.1 } } }, new int[][] { { 0, 0 } }, new int[][] { { 0, 1 } }), new SpikeProtocolSettings(spikeProtocolSettingsPresetNames[2], 2, 1, 60, new int[] { 1, 2 }, new double[][][] { { { 0.05 }, { 0.0, 0.1 } }, { { 0.05 }, { 0.05, 0.1 } }, { { 0.05 }, { 0.0, 0.05 } } }, new int[][] { { 0, 0 }, { 0, 1 } }, new int[][] { { 1, 0 }, { 0, 1 } }), };

	protected static class SpikeProtocolSettings {
		public String name;
		public int variationDimsCount;
		public double period;
		public int repetitions;
		public int[] spikeCounts; // [pre, post]
		public double[][][] patterns; // [initial, dim 1, dim 2][pre, post][spike index] = time
		public int[][] refSpikeIndexes; // [dim 1, dim 2][base, relative] = spike index
		public int[][] refSpikePreOrPost; // [dim 1, dim 2][base, relative] = PRE or POST

		public SpikeProtocolSettings() {
		}

		public SpikeProtocolSettings(String name, int variationDimsCount, double period, int repetitions, int[] spikeCounts, double[][][] patterns, int[][] refSpikeIndexes, int[][] refSpikePreOrPost) {
			this.name = name;
			this.variationDimsCount = variationDimsCount;
			this.period = period;
			this.repetitions = repetitions;
			this.spikeCounts = spikeCounts;
			this.patterns = patterns;
			this.refSpikeIndexes = refSpikeIndexes;
			this.refSpikePreOrPost = refSpikePreOrPost;
		}
	}

	private class SynapseSettingsPanel extends JPanel {
		private static final long serialVersionUID = 1L;

		SynapseCollection synapseSingleton;
		ComponentConfiguration synapseConfig;

		public SynapseSettingsPanel(final STDPTestGUI gui) {
			final JPanel panel = this;
			panel.setLayout(new GridBagLayout());

			String[] synapseTypes = { 
					"com.ojcoleman.bain.synapse.spiking.Clopath2010SynapseCollection", 
					"com.ojcoleman.bain.synapse.spiking.Pfister2006SynapseCollection", 
					"com.ojcoleman.bain.synapse.spiking.Graupner2012SynapseCollection", 
					"com.ojcoleman.bain.synapse.spiking.Graupner2012SimplifiedSynapseCollection" };
			for (String t : synapseTypes) {
				try {
					SynapseCollection.registerComponentCollectionType(t);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}

			final JComboBox<String> synapseSelector = new JComboBox<String>(synapseTypes);
			final JPanel synapseParamsPanel = new JPanel(new GridBagLayout());
			synapseSelector.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					synapseConfig = null;
					synapseParamsPanel.removeAll();
					panel.validate();

					try {
						synapseSingleton = (SynapseCollection) SynapseCollection.getComponentCollectionSingleton((String) synapseSelector.getSelectedItem());
						synapseConfig = synapseSingleton.getConfigSingleton().createConfiguration();
						setupSynapseConfig(synapseParamsPanel, synapseConfig);
						panel.validate();
					} catch (Exception e1) {
						JOptionPane.showMessageDialog(null, "Unable to load selected  type.", "Error", JOptionPane.ERROR_MESSAGE);
						e1.printStackTrace();
						return;
					}
				}
			});
			synapseSelector.setSelectedIndex(0);

			GridBagConstraints gbc = new GridBagConstraints();
			gbc.weightx = 1;
			gbc.weighty = 0;
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			panel.add(createLabeledComponent("Synapse Model: ", synapseSelector), gbc);

			synapseParamsPanel.setBorder(createBorder("Parameters"));
			gbc.gridy = 1;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.weighty = 1;
			panel.add(synapseParamsPanel, gbc);
		}

		private void setupSynapseConfig(JPanel synapseParamsPanel, final ComponentConfiguration config) {
			if (config != null) {
				final String[] paramNames = config.getParameterNames();
				final String[] presetNames = config.getPresetNames();

				if (paramNames != null) {
					GridBagConstraints gbc = new GridBagConstraints();
					gbc.weightx = 1;
					gbc.weighty = 1;
					gbc.gridx = 0;
					gbc.gridy = 0;
					gbc.gridwidth = 2;
					gbc.anchor = GridBagConstraints.NORTH;
					gbc.fill = GridBagConstraints.HORIZONTAL;
					final JFormattedTextField[] fields = new JFormattedTextField[paramNames.length];
					final JComboBox<String> presetSelector = presetNames != null ? new JComboBox<String>(presetNames) : null;

					if (presetSelector != null) {
						presetSelector.addItem("<Custom>");
						final String[] paramNamesFinal = paramNames;
						presetSelector.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								int index = presetSelector.getSelectedIndex();
								// If custom was not selected
								if (index < presetNames.length) {
									try {
										ComponentConfiguration preset = config.getPreset(presetSelector.getSelectedIndex());
										double[] params = preset.getParameterValues();
										for (int pi = 0; pi < params.length; pi++) {
											// Don't set text field if it contains the required value already.
											// This prevents an infinite loop when the text field event
											// listener recognises that the text fields contain the same values a preset
											// and updates the presetSelector accordingly.
											if (fields[pi].getValue() == null || ((Number) fields[pi].getValue()).doubleValue() != params[pi]) {
												fields[pi].setValue(params[pi]);
											}
										}
										config.name = preset.name;
									} catch (Exception ex) {
										ex.printStackTrace();
									}
								}
							}
						});
						synapseParamsPanel.add(presetSelector, gbc);
					}

					gbc.gridwidth = 1;
					int pi = 0;
					Format format = new DecimalFormat("###############0.###############");
					for (String param : paramNames) {
						gbc.gridy++;
						gbc.gridx = 0;
						gbc.weightx = 0.1;
						synapseParamsPanel.add(new JLabel(param), gbc);
						gbc.gridx = 1;
						gbc.weightx = 1;
						fields[pi] = new JFormattedTextField(format);
						final String paramFinal = param;
						final JFormattedTextField field = fields[pi];
						fields[pi].getDocument().addDocumentListener(new DocumentListener() {
							@Override
							public void changedUpdate(DocumentEvent arg0) {
								changeOccurred();
							}

							@Override
							public void insertUpdate(DocumentEvent arg0) {
								changeOccurred();
							}

							@Override
							public void removeUpdate(DocumentEvent arg0) {
								changeOccurred();
							}

							private void changeOccurred() {
								config.setParameterValue(paramFinal, ((Number) field.getValue()).doubleValue(), false);

								if (presetSelector != null) {
									// Determine if the current values correspond to a preset.
									int prsi = config.getMatchingPreset();
									presetSelector.setSelectedIndex(prsi == -1 ? config.getPresetNames().length : prsi);
								}
							}
						});
						synapseParamsPanel.add(fields[pi], gbc);
						pi++;
					}

					if (presetSelector != null) {
						presetSelector.setSelectedIndex(0);
					}
				}
			}
		}

		public SynapseCollection getSynapse() {
			SynapseCollection synapse = (SynapseCollection) synapseSingleton.createCollection(1);
			synapse.addConfiguration(synapseConfig);
			return synapse;
		}
	}

	private static class SpikeTimingSetterPair extends Box {
		private static final long serialVersionUID = 1L;

		boolean allowRefSpikes;
		int baseRefSpike = -1, baseRefPP = -1, relativeRefSpike = -1, relativeRefPP = -1;
		public SpikeTimingSetter pre, post;

		public SpikeTimingSetterPair(boolean allowRefSpikes) {
			super(BoxLayout.Y_AXIS);
			this.allowRefSpikes = allowRefSpikes;
		}

		public void setBaseRef(int preOrPost, int spike) {
			if (allowRefSpikes) {
				baseRefPP = preOrPost;
				baseRefSpike = spike;
				if (baseRefPP == relativeRefPP && baseRefSpike == relativeRefSpike) {
					relativeRefPP = -1;
					relativeRefSpike = -1;
				}
				pre.repaint();
				post.repaint();
			}
		}

		public void setRelativeRef(int preOrPost, int spike) {
			if (allowRefSpikes) {
				relativeRefPP = preOrPost;
				relativeRefSpike = spike;
				if (baseRefPP == relativeRefPP && baseRefSpike == relativeRefSpike) {
					baseRefPP = -1;
					baseRefSpike = -1;
				}
				pre.repaint();
				post.repaint();
			}
		}

		public int getBaseRefSpike() {
			return baseRefSpike;
		}

		public int getBaseRefPreOrPost() {
			return baseRefPP;
		}

		public int getRelativeRefSpike() {
			return relativeRefSpike;
		}

		public int getRelativeRefPreOrPost() {
			return relativeRefPP;
		}

		public void setSpikeTime(int preOrPost, int spike, int value) {
			if (preOrPost == NeuronCollection.PRE) {
				pre.setSpikeTime(spike, value);
			} else if (preOrPost == NeuronCollection.POST) {
				post.setSpikeTime(spike, value);
			}
		}
	}

	private static class SpikeTimingSetter extends Box implements ChangeListener, ThumbListener {
		private static final long serialVersionUID = 1L;

		int spikeCount;
		double period; // in seconds
		int preOrPost;
		protected int lastSelectedSpike = -1;
		JXMultiThumbSlider<Object> timingSlider;
		java.util.Vector<JSpinner> timingSpinners;
		Box timingSpinnersBox;
		SpikeTimingSetterPair pair;

		SpikeTimingSetter(String label, int timingCount, double period, SpikeTimingSetterPair prePostPair, final int preOrPost) {
			super(BoxLayout.Y_AXIS);
			setBorder(createBorder(label));

			this.period = period;
			this.preOrPost = preOrPost;
			this.pair = prePostPair;
			if (preOrPost == NeuronCollection.PRE) {
				pair.pre = this;
			} else if (preOrPost == NeuronCollection.POST) {
				pair.post = this;
			}

			timingSlider = new JXMultiThumbSlider<Object>();
			timingSlider.setMinimumValue(0);
			timingSlider.setMaximumValue((int) (period * 1000));
			timingSlider.setTrackRenderer(new SliderTrackRenderer(this));
			timingSlider.setThumbRenderer(new SliderThumbRenderer(pair, this));
			timingSlider.addMultiThumbListener(this);
			timingSlider.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					lastSelectedSpike = timingSlider.getSelectedIndex();
				}

				public void mouseClicked(MouseEvent e) {
					if (e.getButton() == 1) {
						pair.setBaseRef(preOrPost, lastSelectedSpike);
					} else if (e.getButton() == 3) {
						pair.setRelativeRef(preOrPost, lastSelectedSpike);
					}
				}
			});
			timingSlider.setPreferredSize(new Dimension(200, 20));
			timingSlider.setToolTipText("<html>Set the spike protocol by dragging the spikes. Use the text boxes below to specify exact timings.<br />" + "If one or two variation dimenions are selected then for the purposes of showing spike time deltas in the plot axis you can set a base (cyan) and<br />" + "reference (pink) spike in the final spike protocols by clicking on a spike with the left or right mouse button respectively (see presets for examples).");
			add(timingSlider);

			timingSpinnersBox = new Box(BoxLayout.X_AXIS);
			add(timingSpinnersBox);

			timingSpinners = new java.util.Vector<JSpinner>(timingCount);
			for (int i = 0; i < timingCount; i++) {
				addSpike();
			}
		}

		public int getPreOrPost() {
			return preOrPost;
		}

		public int getSpikeCount() {
			return spikeCount;
		}

		public void setSpikeCount(int spikeCount) {
			if (spikeCount > this.spikeCount) {
				for (int i = this.spikeCount; i < spikeCount; i++) {
					addSpike();
				}
			} else if (spikeCount < this.spikeCount) {
				for (int i = this.spikeCount; i > spikeCount; i--) {
					removeSpike();
				}
			}
		}

		public double getPeriod() {
			return period;
		}

		public void setPeriod(double period) {
			this.period = period;
			int pms = (int) (period * 1000);
			timingSlider.setMaximumValue(pms);
			for (int i = 0; i < spikeCount; i++) {
				if (((SpinnerNumberModel) timingSpinners.get(i).getModel()).getNumber().intValue() > pms) {
					timingSpinners.get(i).getModel().setValue(pms);
				}
				((SpinnerNumberModel) timingSpinners.get(i).getModel()).setMaximum(pms);
			}
		}

		public void addSpike() {
			spikeCount++;
			int newVal = timingSpinners.isEmpty() ? 0 : (getCurrentMaxTimingValue() + (int) (period * 1000)) / 2;
			timingSlider.getModel().addThumb(newVal, null);

			JSpinner newSpinner = new JSpinner(new SpinnerNumberModel(newVal, 0, (int) (period * 1000), 1));
			newSpinner.addChangeListener(this);
			timingSpinners.add(newSpinner);
			timingSpinnersBox.add(newSpinner);
			timingSpinnersBox.validate();
		}

		public void removeSpike() {
			spikeCount--;
			timingSlider.getModel().removeThumb(spikeCount);
			JSpinner oldSpinner = timingSpinners.lastElement();
			timingSpinners.removeElementAt(timingSpinners.size() - 1);
			timingSpinnersBox.remove(oldSpinner);
			timingSpinnersBox.validate();
		}

		public int getCurrentMaxTimingValue() {
			return (spikeCount == 0) ? 0 : ((SpinnerNumberModel) timingSpinners.lastElement().getModel()).getNumber().intValue();
		}

		public double[] getSpikeTimings() {
			double[] timings = new double[spikeCount];
			for (int i = 0; i < spikeCount; i++) {
				timings[i] = ((SpinnerNumberModel) timingSpinners.get(i).getModel()).getNumber().intValue() / 1000d;
			}
			return timings;
		}

		public void setSpikeTime(int spike, int value) {
			timingSlider.getModel().getThumbAt(spike).setPosition(value);
			timingSpinners.get(spike).setValue(value);
		}

		// Interface ChangeListener
		@Override
		public void stateChanged(ChangeEvent e) {
			JSpinner spinner = (JSpinner) e.getSource();
			int index = timingSpinners.indexOf(spinner);
			timingSlider.getModel().getThumbAt(index).setPosition(((SpinnerNumberModel) spinner.getModel()).getNumber().intValue());
			enforceOrdering(index, ((SpinnerNumberModel) spinner.getModel()).getNumber().intValue());
		}

		// Interface ThumbListener
		@Override
		public void mousePressed(MouseEvent e) {
		}

		// Interface ThumbListener
		@Override
		public void thumbMoved(int index, float pos) {
			int value = (int) Math.round(pos);
			timingSpinners.get(index).setValue(value);
			enforceOrdering(index, value);
		}

		// Interface ThumbListener
		@Override
		public void thumbSelected(int thumb) {
		}

		// Make sure spike timings are in same order temporally as their
		// indexes.
		private void enforceOrdering(int fixedIndex, int fixedValue) {
			for (int i = fixedIndex + 1; i < spikeCount; i++) {
				if (((SpinnerNumberModel) timingSpinners.get(i).getModel()).getNumber().intValue() < fixedValue) {
					timingSpinners.get(i).setValue(fixedValue);
					timingSlider.getModel().getThumbAt(i).setPosition(fixedValue);
				}
			}
			for (int i = fixedIndex - 1; i >= 0; i--) {
				if (((SpinnerNumberModel) timingSpinners.get(i).getModel()).getNumber().intValue() > fixedValue) {
					timingSpinners.get(i).setValue(fixedValue);
					timingSlider.getModel().getThumbAt(i).setPosition(fixedValue);
				}
			}
		}

		private static class SliderTrackRenderer extends JComponent implements TrackRenderer {
			private static final long serialVersionUID = 1L;
			private JXMultiThumbSlider<?> slider;
			private SpikeTimingSetter spikeTimingSetter;

			SliderTrackRenderer(SpikeTimingSetter spikeTimingSetter) {
				this.spikeTimingSetter = spikeTimingSetter;
			}

			@Override
			public void paint(Graphics g) {
				super.paint(g);
				paintComponent(g);
			}

			@Override
			protected void paintComponent(Graphics g) {
				g.setColor(Color.BLACK);
				g.fillRect(0, 0, slider.getWidth(), slider.getHeight() - 3);
				// int zeroPoint = (int) Math.round(((double)
				// -spikeTimingSetter.minValue/spikeTimingSetter.period) *
				// slider.getWidth());
				// g.setColor(Color.lightGray); g.drawLine(zeroPoint, 0,
				// zeroPoint, slider.getHeight()-3);
			}

			@Override
			public JComponent getRendererComponent(JXMultiThumbSlider slider) {
				this.slider = slider;
				return this;
			}
		}

		private static class SliderThumbRenderer extends JComponent implements ThumbRenderer {
			private static final long serialVersionUID = 1L;
			SpikeTimingSetterPair spikeTimingSetterPair;
			SpikeTimingSetter spikeTimingSetter;
			JXMultiThumbSlider<?> slider;
			int index;

			public SliderThumbRenderer(SpikeTimingSetterPair prePostPair, SpikeTimingSetter spikeTimingSetter) {
				this.spikeTimingSetterPair = prePostPair;
				this.spikeTimingSetter = spikeTimingSetter;
			}

			protected void paintComponent(Graphics g) {
				if (spikeTimingSetter.getPreOrPost() == spikeTimingSetterPair.getBaseRefPreOrPost() && index == spikeTimingSetterPair.getBaseRefSpike()) {
					g.setColor(Color.CYAN);
				} else if (spikeTimingSetter.getPreOrPost() == spikeTimingSetterPair.getRelativeRefPreOrPost() && index == spikeTimingSetterPair.getRelativeRefSpike()) {
					g.setColor(Color.PINK);
				} else {
					g.setColor(Color.YELLOW);
				}
				// thumb size is currently forced to 10x10, work around to use
				// thumb width of 4.
				g.fillRect(3, 0, 4, slider.getHeight());
			}

			public JComponent getThumbRendererComponent(JXMultiThumbSlider slider, int index, boolean selected) {
				this.slider = slider;
				this.index = index;

				return this;
			}
		}
	}

	private static Border createBorder(String title) {
		return new CompoundBorder(new TitledBorder(title), new EmptyBorder(margin, margin, margin, margin));
	}

	private static Component createLabeledComponent(String label, Component comp) {
		JPanel p = new JPanel();
		p.add(new JLabel(label));
		p.add(comp);
		return p;
	}
}