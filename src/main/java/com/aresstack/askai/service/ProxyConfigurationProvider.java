package com.aresstack.askai.service;

import com.aresstack.winproxy.ProxyConfiguration;

/**
 * Supplies the current AskAI proxy configuration. Implemented by {@code AskAiModel}
 * (via a method reference) so services always read the live configuration rather than a
 * snapshot taken at construction time.
 */
@FunctionalInterface
public interface ProxyConfigurationProvider {

    ProxyConfiguration get();
}
