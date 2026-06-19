package com.aresstack.askai.ui;

import com.aresstack.askai.AskAiModel;
import com.aresstack.askai.client.OllamaClient;
import com.aresstack.askai.client.OllamaModelInfo;
import com.aresstack.askai.client.OllamaModelListResponse;
import com.aresstack.askai.client.OllamaRunningModelInfo;
import com.aresstack.askai.client.OllamaRunningModelListResponse;
import com.aresstack.askai.util.JsonSupport;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

/**
 * Shows installed and currently loaded Ollama models as rich object cards.
 */
public final class OllamaModelsPanel extends JPanel {

    private final AskAiModel model;
    private final JTabbedPane tabs;
    private final JPanel installedCardsPanel;
    private final JPanel runningCardsPanel;
    private final JLabel installedStatusLabel;
    private final JLabel runningStatusLabel;
    private final JLabel informationLabel;
    private boolean refreshedOnce;

    public OllamaModelsPanel(AskAiModel model) {
        this.model = model;
        this.tabs = new JTabbedPane();
        this.installedCardsPanel = createCardsPanel();
        this.runningCardsPanel = createCardsPanel();
        this.installedStatusLabel = new JLabel("Installed models are not loaded yet.");
        this.runningStatusLabel = new JLabel("Running models are not loaded yet.");
        this.informationLabel = new JLabel("Ollama server information is not loaded yet.");
        buildUserInterface();
    }

    public void onShown() {
        if (!refreshedOnce) {
            refreshedOnce = true;
            refreshInstalledModels();
            refreshServerInformation();
            return;
        }
        refreshSelectedTab();
    }

    private void buildUserInterface() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        tabs.addTab("Installed Models", createInstalledModelsTab());
        tabs.addTab("Running Models", createRunningModelsTab());
        tabs.addChangeListener(event -> refreshSelectedTab());
        add(tabs, BorderLayout.CENTER);
        informationLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 4));
        add(informationLabel, BorderLayout.SOUTH);
    }

    private JPanel createInstalledModelsTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(createInstalledToolbar(), BorderLayout.NORTH);
        panel.add(new JScrollPane(installedCardsPanel), BorderLayout.CENTER);
        showInstalledPlaceholder("Open Models or click Refresh to load installed models.");
        return panel;
    }

    private JPanel createRunningModelsTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(createRunningToolbar(), BorderLayout.NORTH);
        panel.add(new JScrollPane(runningCardsPanel), BorderLayout.CENTER);
        showRunningPlaceholder("Switch to this tab to load running models.");
        return panel;
    }

    private JPanel createInstalledToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(event -> refreshInstalledModels());
        toolbar.add(refreshButton);
        toolbar.add(installedStatusLabel);
        return toolbar;
    }

    private JPanel createRunningToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(event -> refreshRunningModels());
        toolbar.add(refreshButton);
        toolbar.add(runningStatusLabel);
        return toolbar;
    }

    private static JPanel createCardsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        return panel;
    }

    private void refreshSelectedTab() {
        if (tabs.getSelectedIndex() == 0) {
            refreshInstalledModels();
        } else if (tabs.getSelectedIndex() == 1) {
            refreshRunningModels();
        }
    }

    private void refreshInstalledModels() {
        installedStatusLabel.setText("Loading installed models from " + model.getOllamaBaseUrl() + " ...");
        showInstalledPlaceholder("Loading installed models ...");
        new SwingWorker<OllamaModelListResponse, Void>() {
            @Override
            protected OllamaModelListResponse doInBackground() throws Exception {
                return new OllamaClient(model.getOllamaBaseUrl()).getInstalledModels();
            }

            @Override
            protected void done() {
                try {
                    List<OllamaModelInfo> models = get().getModels();
                    showInstalledModels(models);
                    installedStatusLabel.setText("Loaded " + models.size() + " installed models.");
                } catch (Exception ex) {
                    showInstalledPlaceholder("Could not load installed models: " + ex.getMessage());
                    installedStatusLabel.setText("Error while loading installed models.");
                }
            }
        }.execute();
    }

    private void refreshRunningModels() {
        runningStatusLabel.setText("Loading running models from " + model.getOllamaBaseUrl() + " ...");
        showRunningPlaceholder("Loading running models ...");
        new SwingWorker<OllamaRunningModelListResponse, Void>() {
            @Override
            protected OllamaRunningModelListResponse doInBackground() throws Exception {
                return new OllamaClient(model.getOllamaBaseUrl()).getRunningModels();
            }

            @Override
            protected void done() {
                try {
                    List<OllamaRunningModelInfo> models = get().getModels();
                    showRunningModels(models);
                    runningStatusLabel.setText("Loaded " + models.size() + " running models.");
                } catch (Exception ex) {
                    showRunningPlaceholder("Could not load running models: " + ex.getMessage());
                    runningStatusLabel.setText("Error while loading running models.");
                }
            }
        }.execute();
    }

    private void refreshServerInformation() {
        informationLabel.setText("Loading Ollama server information ...");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return new OllamaClient(model.getOllamaBaseUrl()).getVersion();
            }

            @Override
            protected void done() {
                try {
                    String json = get();
                    String version = JsonSupport.extractFirstStringValue(json, "version");
                    informationLabel.setText(version.isEmpty()
                            ? "Ollama server: " + model.getOllamaBaseUrl()
                            : "Ollama server: " + model.getOllamaBaseUrl() + " | version " + version);
                } catch (Exception ex) {
                    informationLabel.setText("Ollama server: " + model.getOllamaBaseUrl());
                }
            }
        }.execute();
    }

    private void showInstalledModels(List<OllamaModelInfo> models) {
        installedCardsPanel.removeAll();
        if (models.isEmpty()) {
            addPlaceholder(installedCardsPanel, "No installed models returned by Ollama.");
        } else {
            for (final OllamaModelInfo modelInfo : models) {
                installedCardsPanel.add(OllamaModelCard.installed(modelInfo, new Runnable() {
                    @Override
                    public void run() {
                        confirmAndDelete(modelInfo.getDisplayName());
                    }
                }));
                installedCardsPanel.add(Box.createVerticalStrut(6));
            }
        }
        refreshCards(installedCardsPanel);
    }

    private void showRunningModels(List<OllamaRunningModelInfo> models) {
        runningCardsPanel.removeAll();
        if (models.isEmpty()) {
            addPlaceholder(runningCardsPanel, "No running models returned by Ollama.");
        } else {
            for (OllamaRunningModelInfo modelInfo : models) {
                runningCardsPanel.add(OllamaModelCard.running(modelInfo));
                runningCardsPanel.add(Box.createVerticalStrut(6));
            }
        }
        refreshCards(runningCardsPanel);
    }

    private void showInstalledPlaceholder(String message) {
        installedCardsPanel.removeAll();
        addPlaceholder(installedCardsPanel, message);
        refreshCards(installedCardsPanel);
    }

    private void showRunningPlaceholder(String message) {
        runningCardsPanel.removeAll();
        addPlaceholder(runningCardsPanel, message);
        refreshCards(runningCardsPanel);
    }

    private void confirmAndDelete(final String modelName) {
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete model '" + modelName + "' from Ollama?",
                "Delete model",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        installedStatusLabel.setText("Deleting " + modelName + " ...");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return new OllamaClient(model.getOllamaBaseUrl()).deleteModel(modelName);
            }

            @Override
            protected void done() {
                try {
                    get();
                    installedStatusLabel.setText("Deleted " + modelName + ".");
                    refreshInstalledModels();
                    if (tabs.getSelectedIndex() == 1) {
                        refreshRunningModels();
                    }
                } catch (Exception ex) {
                    installedStatusLabel.setText("Could not delete " + modelName + ": " + ex.getMessage());
                }
            }
        }.execute();
    }

    private static void addPlaceholder(JPanel target, String message) {
        JLabel label = new JLabel(message);
        label.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        target.add(label);
    }

    private static void refreshCards(JPanel panel) {
        panel.revalidate();
        panel.repaint();
    }
}
