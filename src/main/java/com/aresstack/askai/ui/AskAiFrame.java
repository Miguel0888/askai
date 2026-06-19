package com.aresstack.askai.ui;

import com.aresstack.askai.AskAiModel;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;

/**
 * Main frame for provider-based AI chat and model import.
 */
public final class AskAiFrame extends JFrame {

    private final AskAiModel model;

    public AskAiFrame() {
        super("AskAI");
        this.model = new AskAiModel();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 760);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Config", new OllamaConfigPanel(model));
        tabs.addTab("Proxy", new ProxyPanel(model));
        tabs.addTab("Download & Import", new OllamaDownloadImportPanel(model));
        tabs.addTab("Models", new OllamaModelsPanel(model));
        tabs.addTab("Chat", new OllamaChatPanel(model));
        tabs.addTab("About", new OllamaAboutPanel());

        getContentPane().add(tabs, BorderLayout.CENTER);
    }
}
