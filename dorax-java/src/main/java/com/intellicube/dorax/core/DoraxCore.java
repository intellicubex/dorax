package com.intellicube.dorax.core;

import com.intellicube.dorax.client.OpenAiClient;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Core DoraxAgent flow: receive CLI input, call OpenAI, and return text output.
 */
public final class DoraxCore {

    public String handleInput(String input) {
        String prompt = requireInput(input);
        return OpenAiClient.getInstance().complete(prompt);
    }

    public void streamInput(String input, Consumer<String> onTextDelta) {
        String prompt = requireInput(input);
        OpenAiClient.getInstance().streamComplete(prompt, onTextDelta);
    }

    private static String requireInput(String input) {
        String value = Objects.requireNonNull(input, "input").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("input must not be blank");
        }
        return value;
    }
}
