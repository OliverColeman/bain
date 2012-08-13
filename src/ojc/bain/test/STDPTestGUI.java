package ojc.bain.test;

import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.text.Format;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import ojc.bain.base.*;
import ojc.bain.misc.*;
import ojc.bain.neuron.*;
import ojc.bain.synapse.*;

import org.jdesktop.swingx.*;
import org.jdesktop.swingx.multislider.*;
import org.jfree.chart.*;

/**
 * GUI for experimenting with ojc.bain.synapse models.
 * 
 * @author Oliver J. Coleman
 */
public class STDPTestGUI extends JFrame {
	private static final long serialVersionUID = 1L;
	static final int margin = 8;

	SpikeProtocolSettingsPanel spikeSettings;
	SynapseSettingsPanel synapseSettings;

	public STDPTestGUI() {
		super("STDP");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setPreferredSize(new Dimension(1000, 600));

		spikeSettings = new SpikeProtocolSettingsPanel(this);
		synapseSettings = new SynapseSettingsPanel(this);

		final JSpinner timeResolutionSpinner = new JSpinner(new SpinnerNumberModel(1000, 0, 100000, 1));

		JButton goButton = new JButton("Go!");
		goButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SynapseCollection synapse = synapseSettings.getSynapse();
				SpikeProtocolSettings settings = spikeSettings.getSpikeProtocolSettings();

				if (synapse != null && settings != null) {
					int timeResolution = (int) timeResolutionSpinner.getValue();
					boolean logSpikesAndStateVariables = settings.repetitions <= 25;

					TestResults results = SynapseTest.testPattern(synapse, timeResolution, settings.period, settings.repetitions, settings.patterns, settings.refSpikeIndexes, settings.refSpikePreOrPost, logSpikesAndStateVariables);
					JFreeChart resultsPlot = SynapseTest.createChart(results, timeResolution, logSpikesAndStateVariables, true);

					// JFrame plotFrame = new JFrame();
					// plotFrame.add(new ChartPanel(resultsPlot));
					// plotFrame.pack();
					// plotFrame.setVisible(true);
				}
			}
		});

		JTabbedPane tabPane = new JTabbedPane();
		tabPane.addTab("Spiking protocol", spikeSettings);
		tabPane.addTab("Synapse", synapseSettings);

		getContentPane().setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.weightx = 1;
		gbc.weighty = 0.1;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.BOTH;
		getContentPane().add(goButton, gbc);
		gbc.gridx = 1;
		getContentPane().add(createLabeledComponent("Time resolution (steps per second):", timeResolutionSpinner), gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weighty = 1;
		gbc.gridwidth = 2;
		getContentPane().add(tabPane, gbc);

		// Display the window.
		pack();
		setVisible(true);
	}

	private class SpikeProtocolSettingsPanel extends JPanel {
		private static final long serialVersionUID = 1L;

		SpikeTimingSetter[][] spikeTimingSetters;
		SpikeTimingSetterPair[] spikeTimingSetterPairs;
		JSpinner variationDimsSpinner, preSpikeCountSpinner, postSpikeCountSpinner, patternFreqSpinner, patternRepetitionsSpinner;

		public SpikeProtocolSettingsPanel(final STDPTestGUI gui) {
			final int initSpikePatternVariationDimensions = 1, initPreSpikeCount = 1, initPostSpikeCount = initSpikePatternVariationDimensions, initPatternFreq = 1, maxSpikePatternVariationDimensions = 2;
			JPanel panel = this;

			JPanel spikeTimingSetterPanel = new JPanel(new GridBagLayout());
			final SpikeTimingSetterPair[] spikeTimingSetterPairs = new SpikeTimingSetterPair[maxSpikePatternVariationDimensions + 1];
			final SpikeTimingSetter[][] spikeTimingSetters = new SpikeTimingSetter[maxSpikePatternVariationDimensions + 1][2]; // [dimension][pre,
																																// post]

			final JSpinner variationDimsSpinner = new JSpinner(new SpinnerNumberModel(initSpikePatternVariationDimensions, 0, maxSpikePatternVariationDimensions, 1));
			variationDimsSpinner.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					int dimCount = (int) variationDimsSpinner.getValue();
					for (int d = 1; d < maxSpikePatternVariationDimensions + 1; d++) {
						spikeTimingSetterPairs[d].setVisible(dimCount >= d);
					}
				}
			});

			final JSpinner preSpikeCountSpinner = new JSpinner(new SpinnerNumberModel(initPreSpikeCount, 0, 10, 1));
			preSpikeCountSpinner.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					int spikeCount = (int) ((JSpinner) e.getSource()).getValue();
					for (int d = 0; d < spikeTimingSetters.length; d++) {
						spikeTimingSetters[d][0].setSpikeCount(spikeCount);
					}
				}
			});
			final JSpinner postSpikeCountSpinner = new JSpinner(new SpinnerNumberModel(initPostSpikeCount, 0, 10, 1));
			postSpikeCountSpinner.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					int spikeCount = (int) ((JSpinner) e.getSource()).getValue();
					for (int d = 0; d < spikeTimingSetters.length; d++) {
						spikeTimingSetters[d][1].setSpikeCount(spikeCount);
					}
				}
			});
			final JSpinner patternFreqSpinner = new JSpinner(new SpinnerNumberModel(1, 0.1, 100, 0.1));
			patternFreqSpinner.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					double period = 1.0 / (double) patternFreqSpinner.getValue();
					for (int d = 0; d < spikeTimingSetters.length; d++) {
						for (int p = 0; p < spikeTimingSetters[d].length; p++) {
							spikeTimingSetters[d][p].setPeriod(period);
						}
					}
				}
			});
			final JSpinner patternRepetitionsSpinner = new JSpinner(new SpinnerNumberModel(60, 1, 1000, 1));

			JPanel fieldsPanel = new JPanel();
			fieldsPanel.setPreferredSize(new Dimension(1000, 100));
			fieldsPanel.add(createLabeledComponent("# pattern variation dimensions:", variationDimsSpinner));
			fieldsPanel.add(createLabeledComponent("# pre-synaptic spikes:", preSpikeCountSpinner));
			fieldsPanel.add(createLabeledComponent("# post-synaptic spikes:", postSpikeCountSpinner));
			fieldsPanel.add(createLabeledComponent("Pattern presentation frequency (Hz):", patternFreqSpinner));
			fieldsPanel.add(createLabeledComponent("# pattern repetitions:", patternRepetitionsSpinner));

			GridBagConstraints gbc = new GridBagConstraints();
			gbc.weightx = 1;
			gbc.weighty = 1;
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.fill = GridBagConstraints.HORIZONTAL;

			panel.add(fieldsPanel); // , gbc);

			for (int d = 0; d < maxSpikePatternVariationDimensions + 1; d++) {
				spikeTimingSetterPairs[d] = new SpikeTimingSetterPair(d > 0);
				spikeTimingSetterPairs[d].setBorder(createBorder(d == 0 ? "Initial spike pattern" : "Final spike pattern for timing variation dimension " + d));
				for (int p = 0; p < 2; p++) {
					spikeTimingSetters[d][p] = new SpikeTimingSetter(p == 0 ? "Pre" : "Post", p == 0 ? initPreSpikeCount : initPostSpikeCount, 1.0 / initPatternFreq, spikeTimingSetterPairs[d], p);
					spikeTimingSetterPairs[d].add(spikeTimingSetters[d][p]);
				}

				spikeTimingSetterPairs[d].setSpikeTime(NeuronCollection.PRE, 0, 50);
				if (initSpikePatternVariationDimensions == 1) {
					spikeTimingSetterPairs[d].setSpikeTime(NeuronCollection.POST, 0, d == 1 ? 100 : 0);
					if (d > 0) {
						spikeTimingSetterPairs[d].setBaseRef(NeuronCollection.PRE, 0);
						spikeTimingSetterPairs[d].setRelativeRef(NeuronCollection.POST, 0);
					}
				}
				if (initSpikePatternVariationDimensions == 2) {
					spikeTimingSetterPairs[d].setSpikeTime(NeuronCollection.POST, 0, d == 1 ? 50 : 0);
					spikeTimingSetterPairs[d].setSpikeTime(NeuronCollection.POST, 1, d == 2 ? 50 : 100);
					if (d > 0) {
						spikeTimingSetterPairs[d].setBaseRef(NeuronCollection.PRE, 0);
						spikeTimingSetterPairs[d].setRelativeRef(NeuronCollection.POST, d == 1 ? 0 : 1);
					}
				}
				gbc.gridx = d % 2;
				gbc.gridy = d / 2;
				spikeTimingSetterPairs[d].setVisible(d <= initSpikePatternVariationDimensions);
				spikeTimingSetterPanel.add(spikeTimingSetterPairs[d], gbc);
			}

			spikeTimingSetterPanel.setPreferredSize(new Dimension(1000, 400));
			panel.add(spikeTimingSetterPanel);

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
			settings.variationDimsCount = (int) variationDimsSpinner.getValue();
			settings.period = 1.0 / (double) patternFreqSpinner.getValue();
			settings.repetitions = (int) patternRepetitionsSpinner.getValue();
			settings.spikeCounts = new int[2];
			settings.spikeCounts[0] = (int) preSpikeCountSpinner.getValue();
			settings.spikeCounts[1] = (int) postSpikeCountSpinner.getValue();
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
							JOptionPane.showMessageDialog(null, "Please select a base and/or relative spike (left or right click on relevant spike, respectively) in variation dimension " + d + " to specify relevant time delta.", "Error",
									JOptionPane.ERROR_MESSAGE);
							return null;
						}

						// Ensure that a spikes timing only varies over at most
						// one dimension.
						for (int si = 0; si < settings.spikeCounts[p]; si++) {
							// If the spikes time in variation dimension d is
							// different to the initial spike time.
							if (settings.patterns[0][p][si] != settings.patterns[d][p][si]) {
								// If it also differs in another dimension.
								if (variationDimForSpike[p][si] != 0) {
									JOptionPane
											.showMessageDialog(null, "A spikes timing may vary at most over one variation dimension. " + (p == 0 ? "Pre" : "Post") + "-synaptic spike " + (si + 1) + " varies over two.", "Error", JOptionPane.ERROR_MESSAGE);
								}
								variationDimForSpike[p][si] = d;
							}
						}
					}

					double baseRefSpikeTimeInitial = settings.patterns[0][settings.refSpikePreOrPost[d - 1][0]][settings.refSpikeIndexes[d - 1][0]];
					double baseRefSpikeTimeFinal = settings.patterns[d][settings.refSpikePreOrPost[d - 1][0]][settings.refSpikeIndexes[d - 1][0]];
					if (baseRefSpikeTimeInitial != baseRefSpikeTimeFinal) {
						JOptionPane.showMessageDialog(null, "It is recommended that the initial and final base spike times be the same (and only the relative spike time differs).", "Warning", JOptionPane.WARNING_MESSAGE);
					}
				}
			}

			return settings;
		}
	}

	private class SpikeProtocolSettings {
		public int variationDimsCount;
		public double period;
		public int repetitions;
		public int[] spikeCounts; // [pre, post]
		// [initial, dim 1, dim 2][pre, post][spike index]
		public double[][][] patterns;
		public int[][] refSpikeIndexes; // [dim 1, dim 2][pre, post]
		public int[][] refSpikePreOrPost; // [dim 1, dim 2][pre, post]
	}

	private class SynapseSettingsPanel extends JPanel {
		private static final long serialVersionUID = 1L;

		SynapseCollection synapse;
		ComponentConfiguration synapseConfig;

		public SynapseSettingsPanel(final STDPTestGUI gui) {
			final JPanel panel = this;
			panel.setLayout(new GridBagLayout());

			String[] synapseTypes = { "ojc.bain.synapse.Pfister2006SynapseCollection", "ojc.bain.synapse.Graupner2012SynapseCollection" };
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
						SynapseCollection synapseSingleton = (SynapseCollection) SynapseCollection.getComponentCollectionSingleton((String) synapseSelector.getSelectedItem());
						synapse = (SynapseCollection) synapseSingleton.createCollection(1);
						synapseConfig = synapse.getConfigSingleton().createConfiguration();
						synapse.addConfiguration(synapseConfig);
						setupSynapseConfig(synapseParamsPanel, synapseConfig);
						panel.validate();
					} catch (Exception e1) {
						JOptionPane.showMessageDialog(null, "Unable to load selected ojc.bain.synapse type.", "Error", JOptionPane.ERROR_MESSAGE);
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
								config.setParameterValue(paramFinal, ((Number) field.getValue()).doubleValue());

								if (presetSelector != null) {
									// Determine if the current values correspond to a preset.
									int prsi;
									for (prsi = 0; prsi < config.getPresetNames().length; prsi++) {
										if (config.equals(config.getPreset(prsi))) {
											break;
										}
									}
									presetSelector.setSelectedIndex(prsi);
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
				if ((int) timingSpinners.get(i).getModel().getValue() > pms) {
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
			return (spikeCount == 0) ? 0 : (int) timingSpinners.lastElement().getValue();
		}

		public double[] getSpikeTimings() {
			double[] timings = new double[spikeCount];
			for (int i = 0; i < spikeCount; i++) {
				timings[i] = (int) timingSpinners.get(i).getValue() / 1000d;
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
			timingSlider.getModel().getThumbAt(index).setPosition((int) spinner.getValue());
			enforceOrdering(index, (int) spinner.getValue());
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
				if ((int) timingSpinners.get(i).getValue() < fixedValue) {
					timingSpinners.get(i).setValue(fixedValue);
					timingSlider.getModel().getThumbAt(i).setPosition(fixedValue);
				}
			}
			for (int i = fixedIndex - 1; i >= 0; i--) {
				if ((int) timingSpinners.get(i).getValue() > fixedValue) {
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

	public static void main(String[] args) {
		new STDPTestGUI();
	}
}