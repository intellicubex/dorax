package com.intellicube.dorax.cli;

import org.junit.jupiter.api.Test;

import picocli.CommandLine;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DoraxCliTest {

    @Test
    void helpExitsZero() {
        int code = new CommandLine(new DoraxCli()).execute("--help");
        assertEquals(0, code);
    }

    @Test
    void defaultRunExitsZeroAfterExit() {
        InputStream original = System.in;
        try {
            System.setIn(new ByteArrayInputStream("exit\n".getBytes(StandardCharsets.UTF_8)));
            int code = new CommandLine(new DoraxCli()).execute();
            assertEquals(0, code);
        } finally {
            System.setIn(original);
        }
    }
}
