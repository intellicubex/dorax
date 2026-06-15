package com.intellicube.dorax.client;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseErrorEvent;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseOutputMessage;
import com.openai.models.responses.ResponseOutputText;
import com.openai.models.responses.ResponseReasoningSummaryTextDoneEvent;
import com.openai.models.responses.ResponseStreamEvent;
import com.openai.models.responses.ResponseTextDoneEvent;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

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
        ResponseCreateParams params = buildCreateParams(prompt);
        return client.responses().create(params);
    }

    /**
     * Calls the Responses API with streaming enabled and forwards assistant text deltas to {@code onTextDelta}.
     */
    public void streamComplete(String prompt, Consumer<String> onTextDelta) {
        Objects.requireNonNull(onTextDelta, "onTextDelta");
        ResponseCreateParams params = buildCreateParams(prompt);
        StreamState state = new StreamState();
        try (StreamResponse<ResponseStreamEvent> stream = client.responses().createStreaming(params)) {
            stream.stream().forEach(event -> dispatchStreamEvent(event, onTextDelta, state));
        }
        if (!state.emittedChunk && hasText(state.fallbackText)) {
            onTextDelta.accept(state.fallbackText);
        }
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

    private ResponseCreateParams buildCreateParams(String prompt) {
        return ResponseCreateParams.builder()
                .input(requirePrompt(prompt))
                .model(model)
                .build();
    }

    private static void dispatchStreamEvent(
            ResponseStreamEvent event,
            Consumer<String> onTextDelta,
            StreamState state) {
        if (event.error().isPresent()) {
            ResponseErrorEvent err = event.error().get();
            throw new IllegalStateException(err.message());
        }
        if (event.outputTextDelta().isPresent()) {
            String delta = event.outputTextDelta().get().delta();
            if (hasText(delta)) {
                state.emittedChunk = true;
                onTextDelta.accept(delta);
            }
            return;
        }
        if (event.reasoningSummaryTextDelta().isPresent()) {
            String delta = event.reasoningSummaryTextDelta().get().delta();
            if (hasText(delta)) {
                state.emittedChunk = true;
                onTextDelta.accept(delta);
            }
            return;
        }
        if (event.outputTextDone().isPresent()) {
            ResponseTextDoneEvent done = event.outputTextDone().get();
            state.rememberFallback(done.text());
            return;
        }
        if (event.outputItemDone().isPresent()) {
            state.rememberFallback(extractOutputText(event.outputItemDone().get().item()));
            return;
        }
        if (event.completed().isPresent()) {
            state.rememberFallback(extractOutputText(event.completed().get().response()));
            return;
        }
        if (event.reasoningSummaryTextDone().isPresent()) {
            ResponseReasoningSummaryTextDoneEvent done = event.reasoningSummaryTextDone().get();
            state.rememberFallback(done.text());
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isEmpty();
    }

    private static String extractOutputText(Response response) {
        StringBuilder text = new StringBuilder();
        for (ResponseOutputItem item : response.output()) {
            String itemText = extractOutputText(item);
            if (hasText(itemText)) {
                appendWithLineSeparator(text, itemText);
            }
        }
        return text.toString().trim();
    }

    private static String extractOutputText(ResponseOutputItem item) {
        Optional<ResponseOutputMessage> message = item.message();
        if (!message.isPresent()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (ResponseOutputMessage.Content content : message.get().content()) {
            Optional<ResponseOutputText> outputText = content.outputText();
            if (outputText.isPresent()) {
                appendWithLineSeparator(text, outputText.get().text());
            }
        }
        return text.toString().trim();
    }

    private static void appendWithLineSeparator(StringBuilder text, String value) {
        if (!hasText(value)) {
            return;
        }
        if (text.length() > 0) {
            text.append(System.lineSeparator());
        }
        text.append(value);
    }

    private static final class StreamState {
        private boolean emittedChunk;
        private String fallbackText;

        private void rememberFallback(String text) {
            if (hasText(text)) {
                fallbackText = text;
            }
        }
    }
}
