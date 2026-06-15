package com.intellicube.dorax.cli;

import com.intellicube.dorax.core.DoraxCore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Minimal read-eval-print loop for DoraxAgent CLI.
 */
public final class DoraxRepl {

    private static final DoraxCore CORE = new DoraxCore();

    private DoraxRepl() {
    }

    public static void run(boolean verbose) throws IOException {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        writer.print("dorax> ");
        writer.flush();
        String line;
        while ((line = reader.readLine()) != null) {
            if (processLine(line, writer, verbose)) {
                break;
            }
            writer.print("dorax> ");
            writer.flush();
        }
    }

    /**
     * @return true when the REPL should exit
     */
    private static boolean processLine(String line, PrintWriter writer, boolean verbose) {
        if (line == null) {
            return true;
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        if ("exit".equalsIgnoreCase(trimmed) || "quit".equalsIgnoreCase(trimmed)) {
            writer.println("再见 — DoraxAgent 已退出。");
            return true;
        }
        if (verbose) {
            writer.println("[verbose] 收到: " + trimmed);
        }
        try {
            CORE.streamInput(trimmed, chunk -> {
                writer.print(chunk);
                writer.flush();
            });
            writer.println();
        } catch (RuntimeException e) {
            writer.println("dorax: OpenAI 调用失败: " + messageOf(e));
        }
        return false;
    }

    private static String messageOf(RuntimeException e) {
        String message = e.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return e.getClass().getSimpleName();
        }
        return message;
    }
}
