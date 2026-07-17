package com.sherlock.tda;

import com.formdev.flatlaf.FlatDarkLaf;
import com.sherlock.tda.gui.MainWindow;

import javax.swing.*;

public class ThreadDumpAnalyzer {
    
    public static void main(String[] args) {
        // Set system properties for better font rendering
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            System.err.println("Failed to initialize FlatLaf: " + e.getMessage());
        }
        
        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}
