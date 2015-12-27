package com.ojcoleman.bain.gui;

import javax.swing.JApplet;
import javax.swing.SwingUtilities;

public class STDPTestGUIApplet extends JApplet {
	private static final long serialVersionUID = 1L;

	public void init() {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					STDPTestGUI gui = new STDPTestGUI();
					gui.setOpaque(true);
					setContentPane(gui);
				}
			});
		} catch (Exception e) {
			System.err.println("createGUI didn't complete successfully");
		}
	}
}
