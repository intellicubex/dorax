package com.intellicube.dorax.client;

import com.openai.client.OpenAIClient;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.services.blocking.ResponseService;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenAiClientTest {

    @Test
    void createResponseRejectsBlankPromptBeforeCallingOpenAi() {
        OpenAiClient client = new OpenAiClient(failingOpenAiClient(), "custom-compatible-model");

        assertThrows(IllegalArgumentException.class, () -> client.createResponse("   "));
    }

    @Test
    void createResponsePassesPromptAndModelToResponsesApi() {
        OpenAiClient client = new OpenAiClient(capturingOpenAiClient(), "custom-compatible-model");

        CapturedParamsException thrown = assertThrows(
                CapturedParamsException.class,
                () -> client.createResponse("Hello Dorax")
        );

        ResponseCreateParams params = thrown.params;
        assertEquals("Hello Dorax", params.input().get().asText());
        assertEquals("custom-compatible-model", params.model().get().asString());
    }

    private static OpenAIClient failingOpenAiClient() {
        return proxy(OpenAIClient.class, (proxy, method, args) -> {
            throw new AssertionError("OpenAI client should not be called for invalid prompts");
        });
    }

    private static OpenAIClient capturingOpenAiClient() {
        ResponseService responseService = proxy(ResponseService.class, (proxy, method, args) -> {
            if ("create".equals(method.getName()) && args != null && args.length == 1
                    && args[0] instanceof ResponseCreateParams) {
                throw new CapturedParamsException((ResponseCreateParams) args[0]);
            }
            throw new UnsupportedOperationException("Unexpected ResponseService method: " + method.getName());
        });

        return proxy(OpenAIClient.class, (proxy, method, args) -> {
            if ("responses".equals(method.getName())) {
                return responseService;
            }
            throw new UnsupportedOperationException("Unexpected OpenAIClient method: " + method.getName());
        });
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        Object proxy = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
        return type.cast(proxy);
    }

    private static final class CapturedParamsException extends RuntimeException {
        private final ResponseCreateParams params;

        private CapturedParamsException(ResponseCreateParams params) {
            this.params = params;
        }
    }
}
