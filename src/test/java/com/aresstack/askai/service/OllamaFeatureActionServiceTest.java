package com.aresstack.askai.service;

import com.aresstack.askai.AskAiModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the descriptor catalog and the offline (no-network) action branches of the
 * real ollama4j-backed feature action service.
 */
class OllamaFeatureActionServiceTest {

    @Test
    void exposesAllFeatureDescriptors() {
        OllamaFeatureActionService service = new OllamaFeatureActionService(new AskAiModel());
        List<FeatureAction> actions = service.actions();

        assertEquals(7, actions.size());
        assertNotNull(findById(actions, "server-health"));
        assertNotNull(findById(actions, "model-details"));
        assertNotNull(findById(actions, "vision-prompt"));
    }

    @Test
    void visionActionReportsHonestOutOfScopeStatus() throws Exception {
        String message = executeAndAwait("vision-prompt");
        assertFalse(message.toLowerCase().contains("placeholder"),
                "should not pretend to be a future placeholder");
        assertTrue(message.toLowerCase().contains("out of scope") || message.toLowerCase().contains("not wired"),
                "should state the capability is not wired: " + message);
    }

    @Test
    void pullModelExplainsItNeedsAModelName() throws Exception {
        String message = executeAndAwait("pull-model");
        assertTrue(message.toLowerCase().contains("name"), "should mention a model name is required: " + message);
    }

    private static String executeAndAwait(String actionId) throws InterruptedException {
        OllamaFeatureActionService service = new OllamaFeatureActionService(new AskAiModel());
        final AtomicReference<String> captured = new AtomicReference<String>();
        final CountDownLatch latch = new CountDownLatch(1);
        service.execute(actionId, new FeatureActionService.FeatureActionListener() {
            @Override
            public void onAccepted(String title, String message) {
                captured.set(message);
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "listener was not called in time");
        return captured.get();
    }

    private static FeatureAction findById(List<FeatureAction> actions, String id) {
        for (FeatureAction action : actions) {
            if (action.getId().equals(id)) {
                return action;
            }
        }
        return null;
    }
}
