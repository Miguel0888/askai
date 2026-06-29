package com.aresstack.askai.ui;

import com.aresstack.askai.AskAiModel;
import com.aresstack.winproxy.ProxyMode;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JComboBox;
import java.awt.Component;
import java.awt.Container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Locks the proxy-panel contract: there is no separate "Apply to downloads" step, and
 * changing the mode writes straight into the model so Hugging Face operations use it.
 */
class ProxyPanelTest {

    @Test
    void hasNoApplyToDownloadsButton() {
        ProxyPanel panel = new ProxyPanel(new AskAiModel());
        assertNull(findButton(panel, "Apply to downloads"),
                "'Apply to downloads' must no longer exist");
    }

    @Test
    void modeChangeWritesConfigurationToModelImmediately() {
        AskAiModel model = new AskAiModel();
        ProxyPanel panel = new ProxyPanel(model);

        @SuppressWarnings("unchecked")
        JComboBox<ProxyMode> modeCombo = (JComboBox<ProxyMode>) findComboBox(panel);
        assertNotNull(modeCombo, "mode dropdown must exist");

        modeCombo.setSelectedItem(ProxyMode.MANUAL_PROXY);
        assertEquals(ProxyMode.MANUAL_PROXY, model.getProxyConfiguration().getMode());

        modeCombo.setSelectedItem(ProxyMode.DISABLED);
        assertEquals(ProxyMode.DISABLED, model.getProxyConfiguration().getMode());
    }

    private static JButton findButton(Container root, String text) {
        for (Component component : root.getComponents()) {
            if (component instanceof JButton && text.equals(((JButton) component).getText())) {
                return (JButton) component;
            }
            if (component instanceof Container) {
                JButton found = findButton((Container) component, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JComboBox<?> findComboBox(Container root) {
        for (Component component : root.getComponents()) {
            if (component instanceof JComboBox) {
                return (JComboBox<?>) component;
            }
            if (component instanceof Container) {
                JComboBox<?> found = findComboBox((Container) component);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
