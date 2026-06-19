package com.aresstack.askai.ui;

import com.aresstack.askai.AskAiModel;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * Main frame for provider-based AI chat and model installation.
 */
public final class AskAiFrame extends JFrame {

    private final AskAiModel model;
    private final JLabel statusLabel;
    private final JTabbedPane tabs;

    public AskAiFrame() {
        super("AskAI");
        this.model = new AskAiModel();
        this.statusLabel = new JLabel();
        this.tabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1180, 820);
        setMinimumSize(new Dimension(980, 680));
        setLocationRelativeTo(null);
        buildUserInterface();
        updateStatus("Chat");
    }

    private void buildUserInterface() {
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(createHeader(), BorderLayout.NORTH);
        getContentPane().add(createTabs(), BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        JLabel title = new JLabel("AskAI");
        title.setFont(title.getFont().deriveFont(18.0f));
        statusLabel.setText("Ollama · " + model.getOllamaBaseUrl());
        header.add(title, BorderLayout.WEST);
        header.add(statusLabel, BorderLayout.EAST);
        return header;
    }

    private JTabbedPane createTabs() {
        tabs.addTab("Chat", new OllamaChatPanel(model));
        tabs.addTab("Models", new OllamaModelsPanel(model));
        tabs.addTab("Install", new OllamaDownloadImportPanel(model));
        tabs.addTab("Connections", new OllamaConfigPanel(model));
        tabs.addTab("Network", new ProxyPanel(model));
        tabs.addTab("About", new OllamaAboutPanel());
        tabs.addChangeListener(event -> {
            int selectedIndex = tabs.getSelectedIndex();
            if (selectedIndex >= 0) {
                updateStatus(tabs.getTitleAt(selectedIndex));
            }
        });
        return tabs;
    }

    private void updateStatus(String screenName) {
        statusLabel.setText("Ollama · " + model.getOllamaBaseUrl() + " · " + screenName);
    }
}
