package com.aresstack.askai.hub;

/**
 * Signals a failed Hugging Face download through the AskAI adapter.
 */
public final class HuggingFaceDownloadException extends Exception {

    public HuggingFaceDownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
