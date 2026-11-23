package com.pokemontcg.bot;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class BotArchitectureTest {

    private static final List<String> FORBIDDEN_IMPORT_PREFIXES = List.of(
            "import org.springframework",
            "import jakarta.persistence",
            "import com.fasterxml.jackson",
            "import org.hibernate",
            "import com.pokemontcg.catalog",
            "import com.pokemontcg.auth",
            "import com.pokemontcg.deck",
            "import com.pokemontcg.match"
    );

    @Test
    void botPackageContainsOnlyPureJavaWithNoFrameworkOrPersistenceImports() throws IOException {
        Path botRoot = Path.of("src/main/java/com/pokemontcg/bot");
        assertThat(Files.exists(botRoot)).isTrue();

        try (Stream<Path> paths = Files.walk(botRoot)) {
            List<Path> javaFiles = paths.filter(p -> p.toString().endsWith(".java")).toList();
            assertThat(javaFiles).isNotEmpty();

            for (Path file : javaFiles) {
                List<String> lines = Files.readAllLines(file);
                for (String line : lines) {
                    String trimmed = line.trim();
                    for (String forbidden : FORBIDDEN_IMPORT_PREFIXES) {
                        assertThat(trimmed)
                                .as("Forbidden import in %s: %s", file, trimmed)
                                .doesNotStartWith(forbidden);
                    }
                }
            }
        }
    }

    @Test
    void botStrategiesNeverImportOrReferenceGameStateDirectly() throws IOException {
        for (String fileName : List.of("BasicBotStrategy.java", "TacticalBotStrategy.java", "BotCardHelper.java")) {
            Path strategyFile = Path.of("src/main/java/com/pokemontcg/bot/" + fileName);
            String content = Files.readString(strategyFile);

            assertThat(content).doesNotContain("GameState");
            assertThat(content).doesNotContain("PlayerState");
        }
    }
}
