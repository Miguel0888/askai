package com.aresstack.askai.ui;

import com.aresstack.askai.AskAiModel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Main frame for provider-based AI chat and model installation.
 */
public final class AskAiFrame extends JFrame {

    private final AskAiModel model;
    private final CardLayout cardLayout;
    private final JPanel cards;
    private final JLabel statusLabel;
    private final Map<String, JButton> navigationButtons;

    public AskAiFrame() {
        super("AskAI");
        this.model = new AskAiModel();
        this.cardLayout = new CardLayout();
        this.cards = new JPanel(cardLayout);
        this.statusLabel = new JLabel();
        this.navigationButtons = new LinkedHashMap<String, JButton>();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1180, 820);
        setMinimumSize(new Dimension(980, 680));
        setLocationRelativeTo(null);
        buildUserInterface();
        showScreen("Chat");
    }

    private void buildUserInterface() {
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(createHeader(), BorderLayout.NORTH);
        getContentPane().add(createNavigation(), BorderLayout.WEST);
        getContentPane().add(createCards(), BorderLayout.CENTER);
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

    private JPanel createNavigation() {
        JPanel navigation = new JPanel(new GridLayout(0, 1, 0, 4));
        navigation.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        navigation.setPreferredSize(new Dimension(150, 1));
        addNavigationButton(navigation, "Chat");
        addNavigationButton(navigation, "Models");
        addNavigationButton(navigation, "Install");
        addNavigationButton(navigation, "Connections");
        addNavigationButton(navigation, "Network");
        addNavigationButton(navigation, "About");
        return navigation;
    }

    private void addNavigationButton(JPanel navigation, final String screenName) {
        JButton button = new JButton(screenName);
        button.addActionListener(event -> showScreen(screenName));
        navigationButtons.put(screenName, button);
        navigation.add(button);
    }

    private JPanel createCards() {
        cards.add(new OllamaChatPanel(model), "Chat");
        cards.add(new OllamaModelsPanel(model), "Models");
        cards.add(new OllamaDownloadImportPanel(model), "Install");
        cards.add(new OllamaConfigPanel(model), "Connections");
        cards.add(new ProxyPanel(model), "Network");
        cards.add(new OllamaAboutPanel(), "About");
        return cards;
    }

    private void showScreen(String screenName) {
        cardLayout.show(cards, screenName);
        for (Map.Entry<String, JButton> entry : navigationButtons.entrySet()) {
            entry.getValue().setEnabled(!entry.getKey().equals(screenName));
        }
        statusLabel.setText("Ollama · " + model.getOllamaBaseUrl() + " · " + screenName);
    }
}
