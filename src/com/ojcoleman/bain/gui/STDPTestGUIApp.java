package com.ojcoleman.bain.gui;

import javax.swing.JFrame;

public class STDPTestGUIApp extends JFrame {
	private static final long serialVersionUID = 1L;

	public STDPTestGUIApp() {
		super("STDP");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		getContentPane().add(new STDPTestGUI());
		pack();
		setVisible(true);
	}

	public static void main(String[] args) {
		new STDPTestGUIApp();
	}
}
