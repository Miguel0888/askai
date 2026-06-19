package com.aresstack.askai.ui;

import com.aresstack.askai.AskAiModel;
import com.aresstack.askai.client.OllamaClient;
import com.aresstack.askai.util.JsonSupport;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

/**
 * Shows installed and currently loaded Ollama models.
 */
public final class OllamaModelsPanel extends JPanel {

    private final AskAiModel model;
    private final DefaultListModel<String> modelListModel;
    private final JList<String> modelList;
    private final JTextArea detailsArea;
    private final JTextArea logArea;

    public OllamaModelsPanel(AskAiModel model) {
        this.model = model;
        this.modelListModel = new DefaultListModel<String>();
        this.modelList = new JList<String>(modelListModel);
        this.detailsArea = new JTextArea(20, 60);
        this.logArea = new JTextArea(8, 80);
        buildUserInterface();
    }

    private void buildUserInterface() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("Refresh models");
        refreshButton.addActionListener(event -> refreshModels());
        JButton runningButton = new JButton("Show running models");
        runningButton.addActionListener(event -> showRunningModels());
        JButton versionButton = new JButton("Server version");
        versionButton.addActionListener(event -> showVersion());
        JButton deleteButton = new JButton("Delete selected");
        deleteButton.addActionListener(event -> deleteSelectedModel());
        buttons.add(refreshButton);
        buttons.add(runningButton);
        buttons.add(versionButton);
        buttons.add(deleteButton);
        add(buttons, BorderLayout.NORTH);

        detailsArea.setEditable(false);
        logArea.setEditable(false);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(modelList), new JScrollPane(detailsArea));
        splitPane.setDividerLocation(260);
        add(splitPane, BorderLayout.CENTER);
        add(new JScrollPane(logArea), BorderLayout.SOUTH);
    }

    private void refreshModels() {
        append("Loading installed models from " + model.getOllamaBaseUrl());
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return new OllamaClient(model.getOllamaBaseUrl()).getTagsJson();
            }

            @Override
            protected void done() {
                try {
                    String json = get();
                    detailsArea.setText(json);
                    List<String> names = JsonSupport.extractModelNames(json);
                    modelListModel.clear();
                    for (String name : names) {
                        modelListModel.addElement(name);
                    }
                    append("Loaded " + names.size() + " models.");
                } catch (Exception ex) {
                    append("ERROR: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void showRunningModels() {
        append("Loading running models ...");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return new OllamaClient(model.getOllamaBaseUrl()).getRunningModelsJson();
            }

            @Override
            protected void done() {
                try {
                    detailsArea.setText(get());
                    append("Loaded running models.");
                } catch (Exception ex) {
                    append("ERROR: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void showVersion() {
        append("Loading server version ...");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return new OllamaClient(model.getOllamaBaseUrl()).getVersion();
            }

            @Override
            protected void done() {
                try {
                    detailsArea.setText(get());
                    append("Loaded version.");
                } catch (Exception ex) {
                    append("ERROR: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void deleteSelectedModel() {
        final String selected = modelList.getSelectedValue();
        if (selected == null || selected.trim().isEmpty()) {
            append("No model selected.");
            return;
        }
        append("Deleting model from AI server: " + selected + " ...");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return new OllamaClient(model.getOllamaBaseUrl()).deleteModel(selected);
            }

            @Override
            protected void done() {
                try {
                    append("Delete result: " + get());
                    refreshModels();
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
