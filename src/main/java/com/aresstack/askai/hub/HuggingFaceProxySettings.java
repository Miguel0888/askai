package com.aresstack.askai.hub;

import com.aresstack.winproxy.ProxyConfiguration;
import com.aresstack.winproxy.ProxyMode;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;

/**
 * Maps the AskAI {@link ProxyConfiguration} to what the {@code huggingface4j} builder
 * needs: a fixed {@link Proxy} for manual proxy mode, a {@link ProxySelector} for
 * PAC/Windows/system modes, or nothing for direct/disabled.
 *
 * <p>This is the single, network-free translation point so the proxy wiring can be unit
 * tested without contacting Hugging Face.</p>
 */
public final class HuggingFaceProxySettings {

    private final Proxy proxy;
    private final ProxySelector selector;

    private HuggingFaceProxySettings(Proxy proxy, ProxySelector selector) {
        this.proxy = proxy;
        this.selector = selector;
    }

    public static HuggingFaceProxySettings direct() {
        return new HuggingFaceProxySettings(null, null);
    }

    public static HuggingFaceProxySettings from(ProxyConfiguration configuration) {
        if (configuration == null) {
            return direct();
        }
        ProxyMode mode = configuration.getMode();
        if (mode == null || mode == ProxyMode.DISABLED) {
            return direct();
        }
        if (mode == ProxyMode.MANUAL_PROXY) {
            String host = configuration.getManualProxyHost();
            int port = configuration.getManualProxyPort();
            if (host != null && !host.isBlank() && port > 0) {
                return new HuggingFaceProxySettings(
                        new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port)), null);
            }
            return direct();
        }
        return new HuggingFaceProxySettings(null, new WinProxySelector(configuration));
    }

    public boolean isDirect() {
        return proxy == null && selector == null;
    }

    public boolean hasProxy() {
        return proxy != null;
    }

    public boolean hasSelector() {
        return selector != null;
    }

    public Proxy proxy() {
        return proxy;
    }

    public ProxySelector selector() {
        return selector;
    }
}
