package com.aresstack.askai.ui;

import com.aresstack.askai.service.FeatureAction;
import com.aresstack.askai.service.FeatureActionService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.GridLayout;

/**
 * Product-facing click dummy for future ollama4j-backed capabilities.
 */
public final class OllamaActionsPanel extends JPanel {

    private final FeatureActionService featureActionService;
    private final JTextArea statusArea;

    public OllamaActionsPanel(FeatureActionService featureActionService) {
        this.featureActionService = featureActionService;
        this.statusArea = new JTextArea(8, 80);
        buildUserInterface();
    }

    private void buildUserInterface() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel header = new JPanel(new BorderLayout(6, 6));
        JLabel title = new JLabel("Ollama Actions");
        JLabel subtitle = new JLabel("UI placeholders supplied by FeatureActionService. No API call is executed here yet.");
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 2, 10, 10));
        for (FeatureAction action : featureActionService.actions()) {
            grid.add(card(action));
        }
        add(grid, BorderLayout.CENTER);

        statusArea.setEditable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        statusArea.setText("Select an action. Opus can later implement the matching service methods behind these listeners.");
        statusArea.setBorder(BorderFactory.createTitledBorder("Action log"));
        add(statusArea, BorderLayout.SOUTH);
    }

    private JPanel card(final FeatureAction action) {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(action.getTitle()),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        JLabel body = new JLabel("<html>" + action.getDescription() + "</html>");
        JButton button = new JButton("Open");
        button.addActionListener(event -> execute(action.getId()));
        panel.add(body, BorderLayout.CENTER);
        panel.add(button, BorderLayout.SOUTH);
        return panel;
    }

    private void execute(String actionId) {
        featureActionService.execute(actionId, new FeatureActionService.FeatureActionListener() {
            @Override
            public void onAccepted(final String title, final String message) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        statusArea.append("\n" + title + ": " + message);
                    }
                });
            }
        });
    }
}
