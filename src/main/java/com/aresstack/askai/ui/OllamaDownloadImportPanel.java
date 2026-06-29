package com.aresstack.askai.ui;

import com.aresstack.askai.AskAiModel;
import com.aresstack.askai.catalog.OllamaModelCandidate;
import com.aresstack.askai.catalog.OllamaModelCatalog;
import com.aresstack.askai.importing.AskAiModelInstallation;
import com.aresstack.askai.importing.AskAiModelStore;
import com.aresstack.askai.importing.OllamaImportListener;
import com.aresstack.askai.importing.OllamaImportPlan;
import com.aresstack.askai.service.ModelDownloadService;
import com.aresstack.askai.service.ModelInstallService;
import com.aresstack.askai.settings.AskAiPaths;
import com.aresstack.windirectml.workbench.download.DownloadAccessSettings;
import com.aresstack.windirectml.workbench.download.DownloadFolderOpener;
import com.aresstack.windirectml.workbench.download.DownloadOverrideStore;
import com.aresstack.windirectml.workbench.download.ModelDownloadManifest;
import com.aresstack.windirectml.workbench.download.ModelDownloader;
import com.aresstack.windirectml.workbench.panels.DownloadAccessConfigDialog;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.nio.file.Path;
import java.util.List;

/**
 * Reuses the tested DirectML Workbench download code and adds Ollama import actions.
 */
public final class OllamaDownloadImportPanel extends JPanel {

    private final AskAiModel model;
    private final ModelInstallService modelInstallService;
    private final ModelDownloadService modelDownloadService;
    private final DownloadOverrideStore overrideStore;
    private final AskAiModelStore modelStore;
    private final JComboBox<OllamaModelCandidate> candidateCombo;
    private final JTextField targetModelNameField;
    private final JTextField quantizationField;
    private final JCheckBox forceDownloadBox;
    private final JLabel localFolderLabel;
    private final JLabel compatibilityLabel;
    private final JProgressBar progressBar;
    private final JTextArea logArea;

    public OllamaDownloadImportPanel(AskAiModel model, ModelInstallService modelInstallService,
                                     ModelDownloadService modelDownloadService) {
        this.model = model;
        this.modelInstallService = modelInstallService;
        this.modelDownloadService = modelDownloadService;
        this.overrideStore = new DownloadOverrideStore(AskAiPaths.downloadOverridesFile());
        this.modelStore = new AskAiModelStore();
        this.overrideStore.load();
        this.candidateCombo = new JComboBox<OllamaModelCandidate>(
                OllamaModelCatalog.candidates().toArray(new OllamaModelCandidate[0]));
        this.targetModelNameField = new JTextField(34);
        this.quantizationField = new JTextField(model.getDefaultQuantization(), 12);
        this.forceDownloadBox = new JCheckBox("Force download");
        this.localFolderLabel = new JLabel();
        this.compatibilityLabel = new JLabel();
        this.progressBar = new JProgressBar(0, 100);
        this.logArea = new JTextArea(18, 80);
        buildUserInterface();
        updateSelectionDetails();
    }

    private void buildUserInterface() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Install model on AI server"));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;

        addRow(form, constraints, 0, "Model", candidateCombo);
        addRow(form, constraints, 1, "Install as", targetModelNameField);
        addRow(form, constraints, 2, "Quantization", quantizationField);
        addRow(form, constraints, 3, "Download folder", localFolderLabel);
        addRow(form, constraints, 4, "Notes", compatibilityLabel);
        candidateCombo.addActionListener(event -> updateSelectionDetails());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton accessButton = new JButton("Hugging Face Access");
        accessButton.addActionListener(event -> configureAccess());
        JButton openFolderButton = new JButton("Open download folder");
        openFolderButton.addActionListener(event -> openFolder());
        JButton validateButton = new JButton("Validate files");
        validateButton.addActionListener(event -> validateLocalFiles());
        JButton downloadButton = new JButton("Download files");
        downloadButton.addActionListener(event -> downloadSelectedModel(false));
        JButton importButton = new JButton("Install local files");
        importButton.addActionListener(event -> importSelectedModel());
        JButton fullInstallButton = new JButton("Download and install");
        fullInstallButton.addActionListener(event -> downloadSelectedModel(true));

        buttons.add(accessButton);
        buttons.add(openFolderButton);
        buttons.add(validateButton);
        buttons.add(forceDownloadBox);
        buttons.add(downloadButton);
        buttons.add(importButton);
        buttons.add(fullInstallButton);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.add(form, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        progressBar.setStringPainted(true);
        add(progressBar, BorderLayout.SOUTH);

        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);
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

    private void updateSelectionDetails() {
        OllamaModelCandidate candidate = selectedCandidate();
        if (candidate == null) {
            return;
        }
        targetModelNameField.setText(candidate.getDefaultOllamaModelName());
        localFolderLabel.setText(installationFor(candidate).getRootDirectory().toString());
        compatibilityLabel.setText(candidate.getCompatibilityNote());
        append("Selected model: " + candidate.getDisplayName());
        append("Note: " + candidate.getCompatibilityNote());
        append("Ollama import profile: " + candidate.getImportProfile().getDisplayName());
    }

    private void configureAccess() {
        OllamaModelCandidate candidate = selectedCandidate();
        ModelDownloadManifest manifest = effectiveManifest(candidate);
        Window owner = SwingUtilities.getWindowAncestor(this);
        DownloadAccessConfigDialog dialog = new DownloadAccessConfigDialog(owner, manifest,
                overrideStore.accessSettings(manifest.modelId()));
        dialog.setVisible(true);
        if (dialog.isAccepted()) {
            ModelDownloadManifest updatedManifest = manifest.withAllUrls(dialog.getEditedUrls());
            overrideStore.storeOverrides(updatedManifest);
            overrideStore.storeAccessSettings(updatedManifest.modelId(), dialog.getAccessSettings());
            append("Stored Hugging Face access settings for " + updatedManifest.modelId());
        }
    }

    private void openFolder() {
        DownloadFolderOpener.openFolder(installationFor(selectedCandidate()).getRootDirectory(), this::append);
    }

    private void validateLocalFiles() {
        OllamaModelCandidate candidate = selectedCandidate();
        ModelDownloadManifest manifest = effectiveManifest(candidate);
        AskAiModelInstallation installation = installationFor(candidate);
        Path targetDir = installation.getSourceDirectory();
        List<String> missing = ModelDownloader.missingRequiredFiles(manifest, targetDir);
        if (missing.isEmpty()) {
            append("All required local files are present: " + targetDir);
        } else {
            append("Missing local files: " + missing);
        }
    }

    private void downloadSelectedModel(final boolean importAfterDownload) {
        final OllamaModelCandidate candidate = selectedCandidate();
        final ModelDownloadManifest manifest = effectiveManifest(candidate);
        final AskAiModelInstallation installation = installationFor(candidate);
        final Path targetDir = installation.getSourceDirectory();
        final boolean force = forceDownloadBox.isSelected();
        final DownloadAccessSettings accessSettings = overrideStore.accessSettings(manifest.modelId());
        append("Preparing AskAI model folder " + installation.getRootDirectory());
        append("Loading model files from Hugging Face (huggingface4j) into " + targetDir);
        if (accessSettings.hasHuggingFaceToken()) {
            append("Using Hugging Face token " + accessSettings.maskedHuggingFaceToken());
        }
        setProgress(0, "Downloading files");

        try {
            modelStore.prepareInstallation(installation);
        } catch (Exception ex) {
            append("ERROR: " + ex.getMessage());
            setProgress(0, "Download failed");
            return;
        }

        modelDownloadService.download(manifest, targetDir, force, accessSettings.huggingFaceToken(),
                new ModelDownloadService.DownloadListener() {
                    @Override
                    public void onLog(final String message) {
                        onUi(() -> append(message));
                    }

                    @Override
                    public void onProgress(final int percent, final String text) {
                        OllamaDownloadImportPanel.this.setProgress(percent, text);
                    }

                    @Override
                    public void onComplete() {
                        onUi(() -> {
                            append("Download complete: " + targetDir);
                            validateLocalFiles();
                            if (importAfterDownload) {
                                importSelectedModel();
                            } else {
                                setProgress(100, "Files downloaded");
                            }
                        });
                    }

                    @Override
                    public void onError(final Exception ex) {
                        onUi(() -> {
                            append("ERROR: " + ex.getMessage());
                            setProgress(0, "Download failed");
                        });
                    }
                });
    }

    private static void onUi(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    private void importSelectedModel() {
        final OllamaModelCandidate candidate = selectedCandidate();
        final AskAiModelInstallation installation = installationFor(candidate);
        final Path targetDir = installation.getSourceDirectory();
        final String modelName = targetModelNameField.getText().trim();
        final String quantization = quantizationField.getText().trim();
        if (modelName.isEmpty()) {
            append("ERROR: Ollama model name is empty.");
            return;
        }
        append("Installing local files from " + targetDir + " as " + modelName);
        append("AskAI model folder: " + installation.getRootDirectory());
        append("Ollama artifacts folder: " + installation.getOllamaDirectory());
        append("Target AI server: " + model.getOllamaBaseUrl());
        if (!candidate.isRecommendedForSpike()) {
            append("Warning: this model is not marked as the safest install path: " + candidate.getCompatibilityNote());
        }
        setProgress(0, "Installing");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                OllamaImportPlan plan = new OllamaImportPlan(targetDir, modelName, quantization,
                        candidate.getImportProfile(), installation);
                return modelInstallService.install(plan, new OllamaImportListener() {
                    @Override
                    public void onMessage(String message) {
                        append(message);
                    }

                    @Override
                    public void onProgress(int percent, String text) {
                        OllamaDownloadImportPanel.this.setProgress(percent, text);
                    }
                });
            }

            @Override
            protected void done() {
                try {
                    append("Install result: " + get());
                    OllamaDownloadImportPanel.this.setProgress(100, "Installed");
                } catch (Exception ex) {
                    append("ERROR: " + ex.getMessage());
                    OllamaDownloadImportPanel.this.setProgress(0, "Install failed");
                }
            }
        }.execute();
    }

    private OllamaModelCandidate selectedCandidate() {
        return (OllamaModelCandidate) candidateCombo.getSelectedItem();
    }

    private ModelDownloadManifest effectiveManifest(OllamaModelCandidate candidate) {
        return overrideStore.applyOverrides(candidate.getManifest());
    }

    private AskAiModelInstallation installationFor(OllamaModelCandidate candidate) {
        return modelStore.installationFor(model.getModelRoot(), candidate.getManifest());
    }

    private void setProgress(final int value, final String text) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                progressBar.setValue(Math.max(0, Math.min(100, value)));
                progressBar.setString(text == null ? value + "%" : value + "% — " + text);
            }
        });
    }

    private void append(String message) {
        UiSupport.appendLog(logArea, message);
    }
}
