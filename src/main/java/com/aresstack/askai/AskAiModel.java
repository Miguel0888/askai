package com.aresstack.askai;

import com.aresstack.askai.settings.AskAiSettings;
import com.aresstack.askai.settings.AskAiSettingsStore;
import com.aresstack.winproxy.ProxyConfiguration;
import com.aresstack.winproxy.ProxyDefaults;
import com.aresstack.winproxy.ProxyMode;

import java.nio.file.Path;

/**
 * Holds mutable application state shared by Swing panels.
 */
public final class AskAiModel {

    private final AskAiSettingsStore settingsStore;
    private AskAiSettings settings;
    private ProxyConfiguration proxyConfiguration;

    public AskAiModel() {
        this.settingsStore = new AskAiSettingsStore();
        this.settings = settingsStore.load();
        this.proxyConfiguration = defaultProxyConfiguration();
    }

    public String getOllamaBaseUrl() {
        return settings.getOllamaBaseUrl();
    }

    public void setOllamaBaseUrl(String ollamaBaseUrl) {
        settings = settings.withOllamaBaseUrl(ollamaBaseUrl);
    }

    public Path getModelRoot() {
        return settings.getModelRoot();
    }

    public void setModelRoot(Path modelRoot) {
        settings = settings.withModelRoot(modelRoot);
    }

    public String getDefaultQuantization() {
        return settings.getDefaultQuantization();
    }

    public void setDefaultQuantization(String defaultQuantization) {
        settings = settings.withDefaultQuantization(defaultQuantization);
    }

    public String getDefaultKeepAlive() {
        return settings.getDefaultKeepAlive();
    }

    public void setDefaultKeepAlive(String defaultKeepAlive) {
        settings = settings.withDefaultKeepAlive(defaultKeepAlive);
    }

    public ProxyConfiguration getProxyConfiguration() {
        return proxyConfiguration;
    }

    public void setProxyConfiguration(ProxyConfiguration proxyConfiguration) {
        this.proxyConfiguration = proxyConfiguration == null ? defaultProxyConfiguration() : proxyConfiguration;
    }

    private static ProxyConfiguration defaultProxyConfiguration() {
        return ProxyConfiguration.builder()
                .mode(ProxyMode.PAC_URL_POWERSHELL)
                .pacUrlDiscoveryScript(ProxyDefaults.DEFAULT_PAC_URL_DISCOVERY_SCRIPT)
                .build();
    }

    public void saveSettings() {
        settingsStore.save(settings);
    }
}
