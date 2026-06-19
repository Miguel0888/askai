package com.aresstack.askai.ui;

import com.aresstack.askai.AskAiModel;

import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;

/**
 * Main frame for provider-based AI chat and model installation.
 */
public final class AskAiFrame extends JFrame {

    private static final String CHAT_VIEW = "chat";
    private static final String MODELS_VIEW = "models";
    private static final String INSTALL_VIEW = "install";
    private static final String CONNECTIONS_VIEW = "connections";
    private static final String NETWORK_VIEW = "network";
    private static final String ABOUT_VIEW = "about";

    private final AskAiModel model;
    private final JLabel connectionStatusLabel;
    private final CardLayout contentLayout;
    private final JPanel contentPanel;

    public AskAiFrame() {
        super("AskAI");
        this.model = new AskAiModel();
        this.connectionStatusLabel = new JLabel();
        this.contentLayout = new CardLayout();
        this.contentPanel = new JPanel(contentLayout);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1180, 820);
        setMinimumSize(new Dimension(980, 680));
        setLocationRelativeTo(null);
        buildUserInterface();
        showScreen(CHAT_VIEW);
    }

    private void buildUserInterface() {
        setJMenuBar(createMenuBar());
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(createContentPanel(), BorderLayout.CENTER);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createChatMenu());
        menuBar.add(createConfigurationMenu());
        menuBar.add(createHelpMenu());
        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(connectionStatusLabel);
        refreshConnectionStatus(CHAT_VIEW);
        return menuBar;
    }

    private JMenu createChatMenu() {
        JMenu chatMenu = new JMenu("Chat");
        chatMenu.add(createScreenItem("Chat", CHAT_VIEW));
        return chatMenu;
    }

    private JMenu createConfigurationMenu() {
        JMenu configurationMenu = new JMenu("Configuration");
        configurationMenu.add(createScreenItem("Models", MODELS_VIEW));
        configurationMenu.add(createScreenItem("Install", INSTALL_VIEW));
        configurationMenu.add(createScreenItem("Connections", CONNECTIONS_VIEW));
        configurationMenu.add(createScreenItem("Network", NETWORK_VIEW));
        return configurationMenu;
    }

    private JMenu createHelpMenu() {
        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(createScreenItem("About", ABOUT_VIEW));
        return helpMenu;
    }

    private JMenuItem createScreenItem(String title, String screenName) {
        JMenuItem item = new JMenuItem(title);
        item.addActionListener(event -> showScreen(screenName));
        return item;
    }

    private JPanel createContentPanel() {
        contentPanel.add(new OllamaChatPanel(model), CHAT_VIEW);
        contentPanel.add(new OllamaModelsPanel(model), MODELS_VIEW);
        contentPanel.add(new OllamaDownloadImportPanel(model), INSTALL_VIEW);
        contentPanel.add(new OllamaConfigPanel(model), CONNECTIONS_VIEW);
        contentPanel.add(new ProxyPanel(model), NETWORK_VIEW);
        contentPanel.add(new OllamaAboutPanel(), ABOUT_VIEW);
        return contentPanel;
    }

    private void showScreen(String screenName) {
        contentLayout.show(contentPanel, screenName);
        refreshConnectionStatus(screenName);
    }

    private void refreshConnectionStatus(String screenName) {
        connectionStatusLabel.setText("Ollama - " + model.getOllamaBaseUrl() + " - " + resolveScreenTitle(screenName));
    }

    private String resolveScreenTitle(String screenName) {
        if (CHAT_VIEW.equals(screenName)) {
            return "Chat";
        }
        if (MODELS_VIEW.equals(screenName)) {
            return "Models";
        }
        if (INSTALL_VIEW.equals(screenName)) {
            return "Install";
        }
        if (CONNECTIONS_VIEW.equals(screenName)) {
            return "Connections";
        }
        if (NETWORK_VIEW.equals(screenName)) {
            return "Network";
        }
        if (ABOUT_VIEW.equals(screenName)) {
            return "About";
        }
        return "AskAI";
    }
}
