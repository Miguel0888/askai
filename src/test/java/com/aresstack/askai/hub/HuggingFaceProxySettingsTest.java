package com.aresstack.askai.hub;

import com.aresstack.winproxy.ProxyConfiguration;
import com.aresstack.winproxy.ProxyMode;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the AskAI proxy configuration is mapped to what huggingface4j needs:
 * a fixed {@link Proxy} for manual mode, a {@code ProxySelector} for PAC/Windows modes,
 * and nothing for disabled/direct.
 */
class HuggingFaceProxySettingsTest {

    @Test
    void disabledModeIsDirect() {
        HuggingFaceProxySettings settings = HuggingFaceProxySettings.from(
                ProxyConfiguration.builder().mode(ProxyMode.DISABLED).build());

        assertTrue(settings.isDirect());
        assertFalse(settings.hasProxy());
        assertFalse(settings.hasSelector());
    }

    @Test
    void manualModeMapsToJavaProxy() {
        HuggingFaceProxySettings settings = HuggingFaceProxySettings.from(
                ProxyConfiguration.builder()
                        .mode(ProxyMode.MANUAL_PROXY)
                        .manualProxyHost("proxy.corp.local")
                        .manualProxyPort(8080)
                        .build());

        assertTrue(settings.hasProxy());
        assertFalse(settings.hasSelector());
        Proxy proxy = settings.proxy();
        assertEquals(Proxy.Type.HTTP, proxy.type());
        InetSocketAddress address = (InetSocketAddress) proxy.address();
        assertEquals("proxy.corp.local", address.getHostString());
        assertEquals(8080, address.getPort());
    }

    @Test
    void incompleteManualConfigFallsBackToDirect() {
        HuggingFaceProxySettings settings = HuggingFaceProxySettings.from(
                ProxyConfiguration.builder().mode(ProxyMode.MANUAL_PROXY).build());

        assertTrue(settings.isDirect());
    }

    @Test
    void pacAndWindowsModesMapToSelector() {
        for (ProxyMode mode : new ProxyMode[]{
                ProxyMode.PAC_URL_POWERSHELL,
                ProxyMode.PAC_URL_WINDOWS_SETTINGS,
                ProxyMode.WINDOWS_STATIC_PROXY,
                ProxyMode.WINDOWS_NATIVE_PROXY_SETTINGS}) {
            HuggingFaceProxySettings settings = HuggingFaceProxySettings.from(
                    ProxyConfiguration.builder().mode(mode).build());

            assertFalse(settings.hasProxy(), "manual proxy not expected for " + mode);
            assertTrue(settings.hasSelector(), "selector expected for " + mode);
            assertNotNull(settings.selector());
        }
    }
}
