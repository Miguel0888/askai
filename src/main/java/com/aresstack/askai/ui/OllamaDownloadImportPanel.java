package com.aresstack.askai.ui;

import com.aresstack.askai.AskAiModel;
import com.aresstack.askai.catalog.OllamaModelImportProfile;
import com.aresstack.askai.hub.HuggingFaceModelSearchResult;
import com.aresstack.askai.importing.AskAiModelInstallation;
import com.aresstack.askai.importing.AskAiModelStore;
import com.aresstack.askai.importing.OllamaImportListener;
import com.aresstack.askai.importing.OllamaImportPlan;
import com.aresstack.askai.service.ModelDownloadService;
import com.aresstack.askai.service.ModelInstallService;
import com.aresstack.windirectml.workbench.download.DownloadFolderOpener;
import com.aresstack.windirectml.workbench.download.ModelDownloadManifest;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.util.List;

/**
 * Install panel driven by Hugging Face search.
 *
 * <p>The user searches the Hub, picks a repository, and AskAI loads the relevant model
 * files via {@code huggingface4j} — no per-file URLs are shown or edited. Only the
 * settings that genuinely cannot be derived from Hugging Face remain: an optional access
 * token (gated repos), the Ollama target name, the revision/branch, the Ollama import
 * profile (chat template), and an optional force flag.</p>
 */
public final class OllamaDownloadImportPanel extends JPanel {

    private static final String PROFILE_AUTO = "Auto (recommended)";
    private static final String PROFILE_QWEN = "Qwen ChatML";
    private static final String PROFILE_DEFAULT = "Default";

    private final AskAiModel model;
    private final ModelInstallService modelInstallService;
    private final ModelDownloadService modelDownloadService;
    private final AskAiModelStore modelStore;

    private final JTextField searchField;
    private final JButton searchButton;
    private final DefaultListModel<HuggingFaceModelSearchResult> resultsModel;
    private final JList<HuggingFaceModelSearchResult> resultsList;
    private final JTextField repoField;
    private final JTextField revisionField;
    private final JPasswordField tokenField;
    private final JTextField installAsField;
    private final JTextField quantizationField;
    private final JComboBox<String> profileCombo;
    private final JCheckBox forceDownloadBox;
    private final JProgressBar progressBar;
    private final JTextArea logArea;

    public OllamaDownloadImportPanel(AskAiModel model, ModelInstallService modelInstallService,
                                     ModelDownloadService modelDownloadService) {
        this.model = model;
        this.modelInstallService = modelInstallService;
        this.modelDownloadService = modelDownloadService;
        this.modelStore = new AskAiModelStore();
        this.searchField = new JTextField(28);
        this.searchButton = new JButton("Search Hugging Face");
        this.resultsModel = new DefaultListModel<HuggingFaceModelSearchResult>();
        this.resultsList = new JList<HuggingFaceModelSearchResult>(resultsModel);
        this.repoField = new JTextField(30);
        this.revisionField = new JTextField("main", 10);
        this.tokenField = new JPasswordField(24);
        this.installAsField = new JTextField(24);
        this.quantizationField = new JTextField(model.getDefaultQuantization(), 10);
        this.profileCombo = new JComboBox<String>(new String[]{PROFILE_AUTO, PROFILE_QWEN, PROFILE_DEFAULT});
        this.forceDownloadBox = new JCheckBox("Force download");
        this.progressBar = new JProgressBar(0, 100);
        this.logArea = new JTextArea(12, 80);
        buildUserInterface();
    }

    private void buildUserInterface() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buildTop(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        progressBar.setStringPainted(true);
        add(progressBar, BorderLayout.SOUTH);
    }

    private JComponent buildTop() {
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        searchBar.add(new JLabel("Search"));
        searchBar.add(searchField);
        searchBar.add(searchButton);
        searchButton.addActionListener(event -> searchModels());
        searchField.addActionListener(event -> searchModels());

        resultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultsList.setVisibleRowCount(6);
        resultsList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                onResultSelected();
            }
        });
        JScrollPane resultsScroll = new JScrollPane(resultsList);
        resultsScroll.setBorder(BorderFactory.createTitledBorder("Hugging Face models"));

        JPanel top = new JPanel(new BorderLayout(6, 6));
        top.add(searchBar, BorderLayout.NORTH);
        top.add(resultsScroll, BorderLayout.CENTER);
        top.add(buildForm(), BorderLayout.SOUTH);
        return top;
    }

    private JComponent buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Install"));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(3, 4, 3, 4);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;

        addRow(form, constraints, 0, "Repository", repoField);
        addRow(form, constraints, 1, "Revision / branch", revisionField);
        addRow(form, constraints, 2, "HF token (gated, optional)", tokenField);
        addRow(form, constraints, 3, "Install as", installAsField);
        addRow(form, constraints, 4, "Ollama profile", profileCombo);
        addRow(form, constraints, 5, "Quantization", quantizationField);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        buttons.add(forceDownloadBox);
        JButton openFolderButton = new JButton("Open model folder");
        openFolderButton.addActionListener(event -> openModelFolder());
        JButton downloadButton = new JButton("Download");
        downloadButton.addActionListener(event -> startInstall(false));
        JButton fullInstallButton = new JButton("Download and install");
        fullInstallButton.addActionListener(event -> startInstall(true));
        buttons.add(openFolderButton);
        buttons.add(downloadButton);
        buttons.add(fullInstallButton);

        GridBagConstraints buttonConstraints = new GridBagConstraints();
        buttonConstraints.gridx = 0;
        buttonConstraints.gridy = 6;
        buttonConstraints.gridwidth = 2;
        buttonConstraints.anchor = GridBagConstraints.WEST;
        form.add(buttons, buttonConstraints);
        return form;
    }

    private JComponent buildCenter() {
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Log"));
        return scroll;
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

    private void searchModels() {
        final String query = searchField.getText().trim();
        if (query.isEmpty()) {
            append("Enter a search term, e.g. \"qwen2.5 coder 0.5b\".");
            return;
        }
        final String token = token();
        searchButton.setEnabled(false);
        append("Searching Hugging Face for \"" + query + "\" ...");
        new SwingWorker<List<HuggingFaceModelSearchResult>, Void>() {
            @Override
            protected List<HuggingFaceModelSearchResult> doInBackground() throws Exception {
                return modelDownloadService.searchModels(query, token);
            }

            @Override
            protected void done() {
                searchButton.setEnabled(true);
                try {
                    List<HuggingFaceModelSearchResult> results = get();
                    resultsModel.clear();
                    for (HuggingFaceModelSearchResult result : results) {
                        resultsModel.addElement(result);
                    }
                    append("Found " + results.size() + " model(s). Select one to install.");
                } catch (Exception ex) {
                    append("Search failed: " + rootMessage(ex));
                }
            }
        }.execute();
    }

    private void onResultSelected() {
        HuggingFaceModelSearchResult selected = resultsList.getSelectedValue();
        if (selected == null) {
            return;
        }
        repoField.setText(selected.getRepoId());
        installAsField.setText(suggestInstallName(selected.getRepoId()));
        profileCombo.setSelectedItem(PROFILE_AUTO);
        if (selected.isGated()) {
            append("Note: " + selected.getRepoId() + " is gated — an HF token may be required.");
        }
    }

    private void startInstall(final boolean importAfterDownload) {
        final String repoId = repoField.getText().trim();
        if (repoId.isEmpty()) {
            append("Pick a model from the list or type a repository id (e.g. Qwen/Qwen2.5-Coder-0.5B-Instruct).");
            return;
        }
        final String revision = revision();
        final String token = token();
        final boolean force = forceDownloadBox.isSelected();
        append("Resolving files for " + repoId + " @ " + revision + " ...");
        showProgress(0, "Resolving repository files");

        new SwingWorker<ModelDownloadManifest, Void>() {
            @Override
            protected ModelDownloadManifest doInBackground() throws Exception {
                return modelDownloadService.createManifest(repoId, revision, token);
            }

            @Override
            protected void done() {
                try {
                    ModelDownloadManifest manifest = get();
                    if (manifest.files().isEmpty()) {
                        append("No relevant model files found in " + repoId + ".");
                        showProgress(0, "Nothing to download");
                        return;
                    }
                    append("Found " + manifest.files().size() + " relevant file(s) in " + repoId + ".");
                    runDownload(manifest, repoId, revision, token, force, importAfterDownload);
                } catch (Exception ex) {
                    append("Could not resolve repository: " + rootMessage(ex));
                    showProgress(0, "Failed");
                }
            }
        }.execute();
    }

    private void runDownload(final ModelDownloadManifest manifest, final String repoId, final String revision,
                             final String token, final boolean force, final boolean importAfterDownload) {
        final AskAiModelInstallation installation = modelStore.installationFor(model.getModelRoot(), manifest);
        final Path targetDir = installation.getSourceDirectory();
        append("Loading model files via huggingface4j into " + targetDir);
        try {
            modelStore.prepareInstallation(installation);
        } catch (Exception ex) {
            append("ERROR: " + ex.getMessage());
            showProgress(0, "Failed");
            return;
        }
        showProgress(0, "Downloading files");
        modelDownloadService.download(manifest, targetDir, force, token, new ModelDownloadService.DownloadListener() {
            @Override
            public void onLog(final String message) {
                onUi(() -> append(message));
            }

            @Override
            public void onProgress(final int percent, final String text) {
                showProgress(percent, text);
            }

            @Override
            public void onComplete() {
                onUi(() -> {
                    append("Download complete: " + targetDir);
                    if (importAfterDownload) {
                        importModel(installation, targetDir, repoId);
                    } else {
                        showProgress(100, "Files downloaded");
                    }
                });
            }

            @Override
            public void onError(final Exception ex) {
                onUi(() -> {
                    append("ERROR: " + ex.getMessage());
                    showProgress(0, "Download failed");
                });
            }
        });
    }

    private void importModel(final AskAiModelInstallation installation, final Path targetDir, final String repoId) {
        final String modelName = installAsField.getText().trim();
        final String quantization = quantizationField.getText().trim();
        final OllamaModelImportProfile profile = selectedProfile(repoId);
        if (modelName.isEmpty()) {
            append("ERROR: 'Install as' (Ollama model name) is empty.");
            return;
        }
        append("Installing " + targetDir + " as " + modelName + " (profile: " + profile.getDisplayName() + ")");
        append("Target AI server: " + model.getOllamaBaseUrl());
        showProgress(0, "Installing");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                OllamaImportPlan plan = new OllamaImportPlan(targetDir, modelName, quantization, profile, installation);
                return modelInstallService.install(plan, new OllamaImportListener() {
                    @Override
                    public void onMessage(String message) {
                        onUi(() -> append(message));
                    }

                    @Override
                    public void onProgress(int percent, String text) {
                        showProgress(percent, text);
                    }
                });
            }

            @Override
            protected void done() {
                try {
                    append("Install result: " + get());
                    showProgress(100, "Installed");
                } catch (Exception ex) {
                    append("ERROR: " + rootMessage(ex));
                    showProgress(0, "Install failed");
                }
            }
        }.execute();
    }

    private OllamaModelImportProfile selectedProfile(String repoId) {
        Object selected = profileCombo.getSelectedItem();
        if (PROFILE_QWEN.equals(selected)) {
            return OllamaModelImportProfile.qwenChatMl();
        }
        if (PROFILE_DEFAULT.equals(selected)) {
            return OllamaModelImportProfile.plain();
        }
        return repoId.toLowerCase().contains("qwen")
                ? OllamaModelImportProfile.qwenChatMl()
                : OllamaModelImportProfile.plain();
    }

    private void openModelFolder() {
        DownloadFolderOpener.openFolder(model.getModelRoot(), this::append);
    }

    private static String suggestInstallName(String repoId) {
        if (repoId == null || repoId.isBlank()) {
            return "";
        }
        int slash = repoId.lastIndexOf('/');
        String name = slash < 0 ? repoId : repoId.substring(slash + 1);
        return name.toLowerCase() + ":latest";
    }

    private String revision() {
        String revision = revisionField.getText().trim();
        return revision.isEmpty() ? "main" : revision;
    }

    private String token() {
        return new String(tokenField.getPassword()).trim();
    }

    private static String rootMessage(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null ? cause.toString() : cause.getMessage();
    }

    private static void onUi(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    private void showProgress(final int value, final String text) {
        onUi(new Runnable() {
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
