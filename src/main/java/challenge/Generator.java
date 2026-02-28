package challenge;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.concurrent.ThreadLocalRandom;

public class Generator {

    private static final String[] PATHS = {
        "/blog/php-enums",
        "/blog/11-million-rows-in-seconds",
        "/blog/laravel-beyond-crud",
        "/blog/php-81-enums",
        "/blog/a-project-at-stitcher",
        "/blog/php-what-i-dont-like",
        "/blog/new-in-php-81",
        "/blog/new-in-php-82",
        "/blog/new-in-php-83",
        "/blog/new-in-php-84",
        "/blog/generics-in-php",
        "/blog/readonly-classes-in-php-82",
        "/blog/fibers-with-a-grain-of-salt",
        "/blog/php-enum-style-guide",
        "/blog/constructor-promotion-in-php-8",
        "/blog/php-match-or-switch",
        "/blog/named-arguments-in-php-80",
        "/blog/php-enums-and-static-analysis",
        "/blog/short-closures-in-php",
        "/blog/attributes-in-php-8",
        "/blog/typed-properties-in-php-74",
        "/blog/a-letter-to-the-php-community",
        "/blog/union-types-in-php-80",
        "/blog/what-is-new-in-php",
        "/blog/readonly-properties-in-php-82",
        "/blog/nullsafe-operator-in-php",
        "/blog/php-deprecations-84",
        "/blog/property-hooks-in-php-84",
        "/blog/asymmetric-visibility-in-php-84",
        "/blog/crafting-quality-code",
        "/blog/object-oriented-programming",
        "/blog/design-patterns-explained",
        "/blog/functional-programming-in-php",
        "/blog/testing-best-practices",
        "/blog/clean-architecture",
        "/blog/domain-driven-design",
        "/blog/event-sourcing-patterns",
        "/blog/cqrs-explained",
        "/blog/microservices-patterns",
        "/blog/api-design-principles",
        "/blog/rest-vs-graphql",
        "/blog/database-optimization",
        "/blog/caching-strategies",
        "/blog/security-best-practices",
        "/blog/ci-cd-pipelines",
        "/blog/docker-for-developers",
        "/blog/kubernetes-basics",
        "/blog/serverless-architecture",
        "/blog/web-performance-tips",
        "/blog/frontend-frameworks-comparison",
    };

    private static final String DOMAIN = "https://stitcher.io";
    private static final int[] YEARS = {2024, 2025, 2026};

    public static void generate(String path, int count) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path), 8 * 1024 * 1024)) {
            ThreadLocalRandom rng = ThreadLocalRandom.current();

            for (int i = 0; i < count; i++) {
                String blogPath = PATHS[rng.nextInt(PATHS.length)];
                int year = YEARS[rng.nextInt(YEARS.length)];
                int month = rng.nextInt(1, 13);
                int day = rng.nextInt(1, 29);
                int hour = rng.nextInt(0, 24);
                int minute = rng.nextInt(0, 60);
                int second = rng.nextInt(0, 60);

                writer.write(DOMAIN);
                writer.write(blogPath);
                writer.write(',');
                writer.write(String.format("%04d-%02d-%02dT%02d:%02d:%02d+00:00", year, month, day, hour, minute, second));
                writer.newLine();

                if (i % 10_000_000 == 0 && i > 0) {
                    System.err.println("Generated " + i + " rows...");
                }
            }

            writer.flush();
            System.err.println("Generated " + count + " rows to " + path);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate data", e);
        }
    }
}
