package com.aresstack.askai.hub;

import com.aresstack.winproxy.ProxyConfiguration;
import com.aresstack.winproxy.ProxyResult;
import com.aresstack.winproxy.WindowsProxyResolver;

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * A {@link ProxySelector} that resolves each target URL through {@code win-proxy-java}'s
 * {@link WindowsProxyResolver}, using the configured AskAI proxy mode (PAC, Windows
 * settings, static, …). Used for all non-manual proxy modes so HTTPS targets such as
 * {@code https://huggingface.co/...} are resolved per request.
 */
final class WinProxySelector extends ProxySelector {

    private final ProxyConfiguration configuration;

    WinProxySelector(ProxyConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public List<Proxy> select(URI uri) {
        if (uri == null) {
            return Collections.singletonList(Proxy.NO_PROXY);
        }
        try {
            ProxyResult result = new WindowsProxyResolver(configuration).resolve(uri.toString());
            return Collections.singletonList(result.toJavaProxyOrNoProxy());
        } catch (RuntimeException ex) {
            return Collections.singletonList(Proxy.NO_PROXY);
        }
    }

    @Override
    public void connectFailed(URI uri, SocketAddress socketAddress, IOException exception) {
        // No failover list to update; resolution is recomputed per request.
    }
}
