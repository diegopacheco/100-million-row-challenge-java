package challenge;

import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: Main <generate|process> [options]");
            System.err.println("  generate [count]  - Generate test data (default: 1000000)");
            System.err.println("  process           - Process measurements.txt -> output.json");
            System.exit(1);
        }

        String dataDir = "target/data";
        try {
            Files.createDirectories(Path.of(dataDir));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create target/data directory", e);
        }

        String inputFile = dataDir + "/measurements.txt";
        String outputFile = dataDir + "/output.json";

        switch (args[0]) {
            case "generate" -> {
                int count = 1_000_000;
                if (args.length > 1) {
                    count = Integer.parseInt(args[1].replace("_", ""));
                }
                Generator.generate(inputFile, count);
            }
            case "process" -> {
                long start = System.nanoTime();
                Processor.process(inputFile, outputFile);
                double elapsed = (System.nanoTime() - start) / 1_000_000_000.0;
                System.err.printf("Completed in %.3fs%n", elapsed);
            }
            default -> {
                System.err.println("Unknown command: " + args[0]);
                System.exit(1);
            }
        }
    }
}
