package src;

import javax.swing.SwingUtilities;

/**
 * Main entry point for AI Lie Detector Chat Analyzer.
 * Sets up system look and feel, then launches the main UI frame on the EDT.
 */
public class Main {
    public static void main(String[] args) {
        // Launch UI on the Event Dispatch Thread (thread-safe Swing practice)
        SwingUtilities.invokeLater(() -> {
            // Force dark title bar on Windows (Java 17+)
            System.setProperty("sun.java2d.uiScale", "1.0");
            new MainFrame();
        });
    }
}
