package com.intellicube.dorax.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.util.concurrent.Callable;

@Command(
        name = "dorax",
        mixinStandardHelpOptions = true,
        version = "dorax-cli 0.1.0-SNAPSHOT",
        description = "DoraxAgent interactive command line"
)
public final class DoraxCli implements Callable<Integer> {

    @Option(names = {"-v", "--verbose"}, description = "Verbose output")
    private boolean verbose;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new DoraxCli()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        try {
            DoraxBanner.print(System.out);
            DoraxRepl.run(verbose);
            return 0;
        } catch (IOException e) {
            System.err.println("dorax: I/O error: " + e.getMessage());
            return 1;
        }
    }
}
