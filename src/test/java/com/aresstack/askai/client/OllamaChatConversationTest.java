package com.aresstack.askai.client;

import io.github.ollama4j.models.chat.OllamaChatMessage;
import io.github.ollama4j.models.chat.OllamaChatMessageRole;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Verifies multi-turn conversation mapping into ollama4j chat messages: roles map
 * correctly and blank turns are dropped so an empty system prompt is not sent.
 */
class OllamaChatConversationTest {

    @Test
    void mapsRolesAndKeepsOrder() {
        List<OllamaChatTurn> conversation = Arrays.asList(
                OllamaChatTurn.system("be concise"),
                OllamaChatTurn.user("hello"),
                OllamaChatTurn.assistant("hi"),
                OllamaChatTurn.user("how are you?"));

        List<OllamaChatMessage> messages = OllamaResponseMapper.toChatMessages(conversation);

        assertEquals(4, messages.size());
        assertSame(OllamaChatMessageRole.SYSTEM, messages.get(0).getRole());
        assertSame(OllamaChatMessageRole.USER, messages.get(1).getRole());
        assertSame(OllamaChatMessageRole.ASSISTANT, messages.get(2).getRole());
        assertSame(OllamaChatMessageRole.USER, messages.get(3).getRole());
        assertEquals("how are you?", messages.get(3).getResponse());
    }

    @Test
    void dropsBlankTurns() {
        List<OllamaChatTurn> conversation = Arrays.asList(
                OllamaChatTurn.system("   "),
                OllamaChatTurn.user("only message"));

        List<OllamaChatMessage> messages = OllamaResponseMapper.toChatMessages(conversation);

        assertEquals(1, messages.size());
        assertSame(OllamaChatMessageRole.USER, messages.get(0).getRole());
    }

    @Test
    void unknownRoleDefaultsToUser() {
        assertSame(OllamaChatMessageRole.USER, OllamaResponseMapper.toRole("something-else"));
    }
}
