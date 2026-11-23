package com.pokemontcg.engine;

import com.pokemontcg.engine.model.ActiveModifier;
import com.pokemontcg.engine.model.AttackDefinition;
import com.pokemontcg.engine.model.CardDefinition;
import com.pokemontcg.engine.model.CardInstance;
import com.pokemontcg.engine.model.GameState;
import com.pokemontcg.engine.model.PlayerState;
import com.pokemontcg.engine.model.PokemonInPlay;
import com.pokemontcg.engine.model.ResistanceRule;
import com.pokemontcg.engine.model.WeaknessRule;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class EngineArchitectureTest {

    private static final List<String> FORBIDDEN_IMPORT_PREFIXES = List.of(
            "import org.springframework",
            "import jakarta.persistence",
            "import com.fasterxml.jackson",
            "import org.hibernate",
            "import com.pokemontcg.catalog",
            "import com.pokemontcg.auth",
            "import com.pokemontcg.deck",
            "import com.pokemontcg.match",
            "import com.pokemontcg.bot"
    );

    @Test
    void enginePackageContainsOnlyPureJavaWithNoFrameworkOrJacksonImports() throws IOException {
        Path engineRoot = Path.of("src/main/java/com/pokemontcg/engine");
        assertThat(Files.exists(engineRoot)).isTrue();

        try (Stream<Path> paths = Files.walk(engineRoot)) {
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
    void persistedEngineStateRecordsExposeNoGetOrIsPrefixedMethods() {
        List<Class<?>> stateRecords = List.of(
                GameState.class,
                PlayerState.class,
                PokemonInPlay.class,
                ActiveModifier.class,
                CardInstance.class,
                CardDefinition.class,
                AttackDefinition.class,
                WeaknessRule.class,
                ResistanceRule.class
        );

        for (Class<?> clazz : stateRecords) {
            assertThat(clazz.isRecord())
                    .as("%s must be a Java record for clean JSONB serialization", clazz.getSimpleName())
                    .isTrue();

            for (Method method : clazz.getMethods()) {
                if (method.getDeclaringClass() == Object.class || method.getParameterCount() > 0) {
                    continue;
                }
                String name = method.getName();
                assertThat(name)
                        .as("Method %s.%s must not use get* or is* prefix so Jackson serializes only record components",
                                clazz.getSimpleName(), name)
                        .doesNotStartWith("get")
                        .doesNotStartWith("is");
            }
        }
    }
}
