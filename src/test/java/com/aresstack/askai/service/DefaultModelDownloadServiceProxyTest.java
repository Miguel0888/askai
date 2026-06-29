package com.aresstack.askai.service;

import com.aresstack.askai.hub.HuggingFaceDownloadException;
import com.aresstack.askai.hub.HuggingFaceModelLoader;
import com.aresstack.askai.hub.HuggingFaceModelSearchResult;
import com.aresstack.winproxy.ProxyConfiguration;
import com.aresstack.winproxy.ProxyMode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Ensures {@link DefaultModelDownloadService} builds the Hugging Face loader with the
 * proxy configuration that is current at call time (read fresh from the provider), not a
 * snapshot taken at construction.
 */
class DefaultModelDownloadServiceProxyTest {

    @Test
    void buildsLoaderWithCurrentProxyConfiguration() throws Exception {
        ProxyConfiguration manual = ProxyConfiguration.builder()
                .mode(ProxyMode.MANUAL_PROXY).manualProxyHost("proxy.local").manualProxyPort(3128).build();
        AtomicReference<ProxyConfiguration> current = new AtomicReference<ProxyConfiguration>(manual);
        AtomicReference<ProxyConfiguration> captured = new AtomicReference<ProxyConfiguration>();

        DefaultModelDownloadService.LoaderFactory factory = (token, proxy) -> {
            captured.set(proxy);
            return new HuggingFaceModelLoader(token, proxy) {
                @Override
                public List<HuggingFaceModelSearchResult> searchModels(String query, int limit)
                        throws HuggingFaceDownloadException {
                    return List.of();
                }
            };
        };

        DefaultModelDownloadService service = new DefaultModelDownloadService(current::get, factory);

        service.searchModels("qwen", "");
        assertSame(manual, captured.get(), "loader must be built with the current proxy configuration");

        // Changing the live configuration must be picked up on the next call.
        ProxyConfiguration direct = ProxyConfiguration.builder().mode(ProxyMode.DISABLED).build();
        current.set(direct);
        service.searchModels("gemma", "");
        assertSame(direct, captured.get(), "service must re-read the proxy configuration per call");
    }
}
