package com.intellicube.dorax.client;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseOutputMessage;
import com.openai.models.responses.ResponseOutputText;

import java.util.Objects;
import java.util.Optional;

/**
 * Small wrapper around the OpenAI Java SDK for basic text generation calls.
 */
public final class OpenAiClient {

    private static final String DEFAULT_MODEL = "openai/gpt-5.5";

    private static volatile OpenAiClient instance;

    private final OpenAIClient client;
    private final String model;

    private OpenAiClient() {
        this(OpenAIOkHttpClient.fromEnv(), DEFAULT_MODEL);
    }

    OpenAiClient(OpenAIClient client, String model) {
        this.client = Objects.requireNonNull(client, "client");
        this.model = requireModel(model);
    }

    public static OpenAiClient getInstance() {
        OpenAiClient current = instance;
        if (current == null) {
            synchronized (OpenAiClient.class) {
                current = instance;
                if (current == null) {
                    current = new OpenAiClient();
                    instance = current;
                }
            }
        }
        return current;
    }

    public static OpenAiClient fromEnv() {
        return getInstance();
    }

    public Response createResponse(String prompt) {
        ResponseCreateParams params = ResponseCreateParams.builder()
                .input(requirePrompt(prompt))
                .model(model)
                .build();
        return client.responses().create(params);
    }

    public String complete(String prompt) {
        return extractOutputText(createResponse(prompt));
    }

    public void close() {
        client.close();
    }

    private static String requirePrompt(String prompt) {
        String value = Objects.requireNonNull(prompt, "prompt");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("prompt must not be blank");
        }
        return value;
    }

    private static String requireModel(String model) {
        String value = Objects.requireNonNull(model, "model").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("model must not be blank");
        }
        return value;
    }

    private static String extractOutputText(Response response) {
        StringBuilder text = new StringBuilder();
        for (ResponseOutputItem item : response.output()) {
            Optional<ResponseOutputMessage> message = item.message();
            if (!message.isPresent()) {
                continue;
            }
            for (ResponseOutputMessage.Content content : message.get().content()) {
                Optional<ResponseOutputText> outputText = content.outputText();
                if (!outputText.isPresent()) {
                    continue;
                }
                if (text.length() > 0) {
                    text.append(System.lineSeparator());
                }
                text.append(outputText.get().text());
            }
        }
        return text.toString().trim();
    }
}
