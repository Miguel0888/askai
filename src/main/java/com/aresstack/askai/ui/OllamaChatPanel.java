package com.aresstack.askai.ui;

import com.aresstack.askai.AskAiModel;
import com.aresstack.askai.service.OllamaService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

/**
 * Main chat panel for the selected AI server model.
 */
public final class OllamaChatPanel extends JPanel {

    private final AskAiModel model;
    private final OllamaService ollamaService;
    private final JComboBox<String> modelCombo;
    private final JTextField keepAliveField;
    private final JTextArea systemPromptArea;
    private final JTextArea promptArea;
    private final JTextArea responseArea;
    private final JTextArea logArea;
    private final JLabel statusLabel;
    private final JButton sendButton;
    private final JButton stopButton;
    private OllamaService.Task chatTask;
    private Timer elapsedTimer;
    private long requestStartedAtMillis;

    public OllamaChatPanel(AskAiModel model, OllamaService ollamaService) {
        this.model = model;
        this.ollamaService = ollamaService;
        this.modelCombo = new JComboBox<String>();
        this.keepAliveField = new JTextField(model.getDefaultKeepAlive(), 10);
        this.systemPromptArea = new JTextArea("You are a concise local assistant.", 3, 70);
        this.promptArea = new JTextArea("", 6, 70);
        this.responseArea = new JTextArea(16, 70);
        this.logArea = new JTextArea(6, 70);
        this.statusLabel = new JLabel("Select a model and send a message.");
        this.sendButton = new JButton("Send");
        this.stopButton = new JButton("Stop");
        buildUserInterface();
        setBusy(false);
        refreshModels();
    }

    private void buildUserInterface() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Chat"));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        addRow(form, constraints, 0, "Model", modelCombo);
        addRow(form, constraints, 1, "Keep alive", keepAliveField);
        addTextRow(form, constraints, 2, "System", systemPromptArea);
        addTextRow(form, constraints, 3, "Message", promptArea);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshModelsButton = new JButton("Refresh models");
        refreshModelsButton.addActionListener(event -> refreshModels());
        sendButton.addActionListener(event -> sendChat());
        stopButton.addActionListener(event -> stopChat());
        buttons.add(refreshModelsButton);
        buttons.add(sendButton);
        buttons.add(stopButton);
        buttons.add(statusLabel);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.add(form, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        responseArea.setEditable(false);
        responseArea.setLineWrap(true);
        responseArea.setWrapStyleWord(true);
        logArea.setEditable(false);
        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.add(new JScrollPane(responseArea), BorderLayout.CENTER);
        center.add(new JScrollPane(logArea), BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);
    }

    private void addRow(JPanel form, GridBagConstraints constraints, int row, String label, java.awt.Component field) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0.0d;
        form.add(new JLabel(label), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1.0d;
        form.add(field, constraints);
    }

    private void addTextRow(JPanel form, GridBagConstraints constraints, int row, String label, JTextArea area) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0.0d;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        form.add(new JLabel(label), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1.0d;
        constraints.fill = GridBagConstraints.BOTH;
        form.add(new JScrollPane(area), constraints);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
    }

    private void refreshModels() {
        setStatus("Loading models from " + model.getOllamaBaseUrl() + " ...");
        ollamaService.listModelNames(new OllamaService.ModelNamesListener() {
            @Override
            public void onModelNames(final List<String> names) {
                onUi(new Runnable() {
                    @Override
                    public void run() {
                        modelCombo.removeAllItems();
                        for (String name : names) {
                            modelCombo.addItem(name);
                        }
                        if (names.isEmpty()) {
                            setStatus("No models installed on this AI server. Open Install to add one.");
                        } else {
                            setStatus("Ready. " + names.size() + " model(s) available.");
                        }
                    }
                });
            }

            @Override
            public void onError(final Exception ex) {
                onUi(new Runnable() {
                    @Override
                    public void run() {
                        setStatus("Server offline or not reachable. Open Connections to fix it.");
                        append("Connection error: " + ex.getMessage());
                    }
                });
            }
        });
    }

    private void sendChat() {
        final String modelName = (String) modelCombo.getSelectedItem();
        if (modelName == null || modelName.trim().isEmpty()) {
            setStatus("No model selected. Open Models or Install first.");
            return;
        }
        final String userPrompt = promptArea.getText().trim();
        if (userPrompt.isEmpty()) {
            setStatus("Write a message before sending.");
            return;
        }
        responseArea.setText("");
        append("Sending message to " + modelName + " on " + model.getOllamaBaseUrl());
        startElapsedTimer();
        setBusy(true);

        OllamaService.ChatRequest request = new OllamaService.ChatRequest(
                modelName, systemPromptArea.getText(), userPrompt, keepAliveField.getText());
        chatTask = ollamaService.streamChat(request, new OllamaService.ChatListener() {
            @Override
            public void onContent(final String content) {
                onUi(new Runnable() {
                    @Override
                    public void run() {
                        responseArea.append(content);
                    }
                });
            }

            @Override
            public void onStatus(final String status) {
                onUi(new Runnable() {
                    @Override
                    public void run() {
                        setStatus(status);
                    }
                });
            }

            @Override
            public void onComplete(final OllamaService.ChatResult result) {
                onUi(new Runnable() {
                    @Override
                    public void run() {
                        stopElapsedTimer();
                        setBusy(false);
                        if (responseArea.getText().trim().isEmpty() && !result.getFallbackText().isEmpty()) {
                            responseArea.setText(result.getFallbackText());
                        }
                        appendMetrics(result);
                        setStatus("Ready.");
                    }
                });
            }

            @Override
            public void onError(final Exception ex) {
                onUi(new Runnable() {
                    @Override
                    public void run() {
                        stopElapsedTimer();
                        setBusy(false);
                        setStatus("Chat failed.");
                        append("ERROR: " + ex.getMessage());
                    }
                });
            }
        });
    }

    private void stopChat() {
        if (chatTask != null) {
            chatTask.cancel();
            stopElapsedTimer();
            setBusy(false);
            setStatus("Cancelled.");
            append("Chat request cancelled.");
        }
    }

    private void startElapsedTimer() {
        requestStartedAtMillis = System.currentTimeMillis();
        elapsedTimer = new Timer(1000, event -> {
            long seconds = (System.currentTimeMillis() - requestStartedAtMillis) / 1000L;
            setStatus("Waiting for first/next token ... " + seconds + "s. CPU-only models can take a while.");
        });
        elapsedTimer.start();
    }

    private void stopElapsedTimer() {
        if (elapsedTimer != null) {
            elapsedTimer.stop();
            elapsedTimer = null;
        }
    }

    private void appendMetrics(OllamaService.ChatResult result) {
        if (result == null || !result.hasMetrics()) {
            append("Chat complete.");
            return;
        }
        double tokens = result.getEvalCount();
        double seconds = result.getEvalDurationNanos() / 1_000_000_000.0d;
        if (seconds > 0.0d) {
            append(String.format("Chat complete: %.0f output tokens, %.2f tok/s.", tokens, tokens / seconds));
        } else {
            append("Chat complete: " + result.getEvalCount() + " output tokens.");
        }
    }

    private void setBusy(boolean busy) {
        sendButton.setEnabled(!busy);
        stopButton.setEnabled(busy);
        modelCombo.setEnabled(!busy);
    }

    private void setStatus(String status) {
        statusLabel.setText(status);
    }

    private void append(String message) {
        UiSupport.appendLog(logArea, message);
    }

    private static void onUi(Runnable runnable) {
        SwingUtilities.invokeLater(runnable);
    }
}
