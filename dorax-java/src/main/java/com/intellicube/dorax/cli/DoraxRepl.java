package com.intellicube.dorax.cli;

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
        writer.println("(DoraxAgent) 已记录: " + trimmed + " — 完整对话能力即将接入。");
        return false;
    }
}
