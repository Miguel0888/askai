package com.aresstack.askai.ui;

import com.aresstack.askai.AskAiModel;
import com.aresstack.askai.client.OllamaClient;
import com.aresstack.askai.util.JsonSupport;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

/**
 * Minimal chat panel for validating an imported Ollama model.
 */
public final class OllamaChatPanel extends JPanel {

    private final AskAiModel model;
    private final JComboBox<String> modelCombo;
    private final JTextField keepAliveField;
    private final JTextArea systemPromptArea;
    private final JTextArea promptArea;
    private final JTextArea responseArea;
    private final JTextArea logArea;

    public OllamaChatPanel(AskAiModel model) {
        this.model = model;
        this.modelCombo = new JComboBox<String>();
        this.keepAliveField = new JTextField(model.getDefaultKeepAlive(), 10);
        this.systemPromptArea = new JTextArea("You are a concise local assistant.", 3, 70);
        this.promptArea = new JTextArea("Antworte nur mit: Remote-Ollama funktioniert.", 6, 70);
        this.responseArea = new JTextArea(14, 70);
        this.logArea = new JTextArea(7, 70);
        buildUserInterface();
    }

    private void buildUserInterface() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Chat request"));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.0d;
        form.add(new JLabel("Model"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1.0d;
        form.add(modelCombo, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 0.0d;
        form.add(new JLabel("keep_alive"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1.0d;
        form.add(keepAliveField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.weightx = 0.0d;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        form.add(new JLabel("System"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1.0d;
        form.add(new JScrollPane(systemPromptArea), constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.weightx = 0.0d;
        form.add(new JLabel("Prompt"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1.0d;
        form.add(new JScrollPane(promptArea), constraints);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshModelsButton = new JButton("Refresh Models");
        refreshModelsButton.addActionListener(event -> refreshModels());
        JButton sendButton = new JButton("Send /api/chat");
        sendButton.addActionListener(event -> sendChat());
        buttons.add(refreshModelsButton);
        buttons.add(sendButton);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.add(form, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        responseArea.setEditable(false);
        logArea.setEditable(false);
        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.add(new JScrollPane(responseArea), BorderLayout.CENTER);
        center.add(new JScrollPane(logArea), BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);
    }

    private void refreshModels() {
        append("Loading models from " + model.getOllamaBaseUrl());
        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return new OllamaClient(model.getOllamaBaseUrl()).getModelNames();
            }

            @Override
            protected void done() {
                try {
                    List<String> names = get();
                    modelCombo.removeAllItems();
                    for (String name : names) {
                        modelCombo.addItem(name);
                    }
                    append("Loaded " + names.size() + " models.");
                } catch (Exception ex) {
                    append("ERROR: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void sendChat() {
        final String modelName = (String) modelCombo.getSelectedItem();
        if (modelName == null || modelName.trim().isEmpty()) {
            append("No model selected.");
            return;
        }
        final String systemPrompt = systemPromptArea.getText();
        final String userPrompt = promptArea.getText();
        final String keepAlive = keepAliveField.getText();
        append("Sending chat request to " + modelName + " ...");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return new OllamaClient(model.getOllamaBaseUrl()).chat(modelName, systemPrompt, userPrompt, keepAlive);
            }

            @Override
            protected void done() {
                try {
                    String json = get();
                    String content = JsonSupport.extractChatMessageContent(json);
                    responseArea.setText(content.isEmpty() ? json : content + System.lineSeparator()
                            + System.lineSeparator() + "--- raw ---" + System.lineSeparator() + json);
                    append("Chat complete.");
                } catch (Exception ex) {
                    append("ERROR: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void append(String message) {
        UiSupport.appendLog(logArea, message);
    }
}
