package com.aresstack.askai;

import com.aresstack.askai.ui.AskAiFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Starts the AskAI spike application.
 */
public final class AskAiApp {

    private AskAiApp() {
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Keep Swing defaults when the platform look and feel is unavailable.
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                AskAiFrame frame = new AskAiFrame();
                frame.setVisible(true);
            }
        });
    }
}
