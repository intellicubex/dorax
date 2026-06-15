package com.intellicube.dorax.cli;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * ASCII banner for DoraxAgent CLI.
 */
public final class DoraxBanner {

    private static final String[] LINES = {
            "",
            "      ____    ___    ____      _   __  __",
            "     |  _ \\  / _ \\  |  _ \\    / \\  \\ \\/ /",
            "     | | | || | | | | |_) |  / _ \\   >  < ",
            "     |____/  \\___/  |_| \\_\\ /_/ \\_\\ /_/\\_\\",
            "",
            "     DoraxAgent :: Intelligent Command Line Agent",
            "     输入指令与智能体交互，输入 exit 或 quit 退出",
            ""
    };

    private DoraxBanner() {
    }

    public static void print(PrintWriter out) {
        for (String line : LINES) {
            out.println(line);
        }
        out.flush();
    }

    public static void print(PrintStream out) {
        for (String line : LINES) {
            out.println(line);
        }
    }
}
