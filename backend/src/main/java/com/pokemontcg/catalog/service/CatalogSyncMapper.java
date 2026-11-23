package com.pokemontcg.catalog.service;

import com.pokemontcg.catalog.client.FetchedCardPayload;
import com.pokemontcg.catalog.client.TcgdexCardDetailResponse;
import com.pokemontcg.catalog.client.TcgdexSetResponse;
import com.pokemontcg.catalog.domain.CardAttack;
import com.pokemontcg.catalog.domain.CardTypeModifier;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CatalogSyncMapper {

    private static final Pattern DIGITS_PATTERN = Pattern.compile("(\\d+)");

    public PreparedSetData toPreparedSet(TcgdexSetResponse response, Instant syncedAt) {
        String series = response.serie() != null ? response.serie().name() : null;
        Integer totalCards = response.cardCount() != null
                ? response.cardCount().total()
                : (response.cards() != null ? response.cards().size() : null);

        return new PreparedSetData(
                response.id(),
                response.name(),
                series,
                parseDate(response.releaseDate()),
                totalCards,
                normalizeLogoUrl(response.logo()),
                syncedAt
        );
    }

    public PreparedCardData toPreparedCard(FetchedCardPayload payload, String setId) {
        TcgdexCardDetailResponse card = payload.detail();
        String category = normalizeCategory(card.category());
        Set<String> types = card.types() != null ? new LinkedHashSet<>(card.types()) : Set.of();

        List<CardAttack> attacks = card.attacks() == null ? List.of() : card.attacks().stream()
                .map(a -> new CardAttack(
                        a.cost() != null ? List.copyOf(a.cost()) : List.of(),
                        a.name(),
                        a.effect(),
                        a.damage()
                ))
                .toList();

        List<CardTypeModifier> weaknesses = mapModifiers(card.weaknesses());
        List<CardTypeModifier> resistances = mapModifiers(card.resistances());

        String effectText = hasText(card.effect()) ? card.effect()
                : (hasText(card.description()) ? card.description() : null);

        return new PreparedCardData(
                card.id(),
                setId,
                card.localId(),
                parseSortOrder(card.localId()),
                card.name(),
                category,
                card.hp(),
                normalizeStage(card.stage()),
                card.evolveFrom(),
                card.retreat(),
                card.trainerType(),
                normalizeEnergyType(category, card.energyType(), effectText),
                card.rarity(),
                card.illustrator(),
                normalizeCardImageUrl(card.image(), "low"),
                normalizeCardImageUrl(card.image(), "high"),
                types,
                attacks,
                weaknesses,
                resistances,
                effectText,
                payload.rawPayload() != null ? payload.rawPayload() : Map.of()
        );
    }

    static int parseSortOrder(String localId) {
        if (!hasText(localId)) {
            return Integer.MAX_VALUE;
        }
        Matcher matcher = DIGITS_PATTERN.matcher(localId);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return Integer.MAX_VALUE;
            }
        }
        return Integer.MAX_VALUE;
    }

    static String normalizeCardImageUrl(String baseImage, String quality) {
        if (!hasText(baseImage)) {
            return null;
        }
        String trimmed = baseImage.trim();
        if (trimmed.endsWith(".webp") || trimmed.endsWith(".png") || trimmed.endsWith(".jpg")) {
            return trimmed;
        }
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + "/" + quality + ".webp";
    }

    static String normalizeEnergyType(String category, String rawEnergyType, String effectText) {
        if (!"Energy".equalsIgnoreCase(category)) {
            return null;
        }
        if ("Special".equalsIgnoreCase(rawEnergyType) || hasText(effectText)) {
            return "Special";
        }
        return "Basic";
    }

    static String normalizeStage(String stage) {
        if (!hasText(stage)) {
            return null;
        }
        String compact = stage.trim().replace(" ", "");
        if ("Basic".equalsIgnoreCase(compact)) {
            return "Basic";
        }
        if ("Stage1".equalsIgnoreCase(compact)) {
            return "Stage1";
        }
        if ("Stage2".equalsIgnoreCase(compact)) {
            return "Stage2";
        }
        return stage.trim();
    }

    private static String normalizeCategory(String category) {
        if (!hasText(category)) {
            return "Unknown";
        }
        String trimmed = category.trim();
        if ("Pokemon".equalsIgnoreCase(trimmed) || "Pokémon".equalsIgnoreCase(trimmed)) {
            return "Pokemon";
        }
        if ("Trainer".equalsIgnoreCase(trimmed)) {
            return "Trainer";
        }
        if ("Energy".equalsIgnoreCase(trimmed)) {
            return "Energy";
        }
        return trimmed;
    }

    private static List<CardTypeModifier> mapModifiers(List<TcgdexCardDetailResponse.TcgdexTypeModifier> list) {
        if (list == null) {
            return List.of();
        }
        return list.stream()
                .map(m -> new CardTypeModifier(m.type(), m.value()))
                .toList();
    }

    private static LocalDate parseDate(String rawDate) {
        if (!hasText(rawDate)) {
            return null;
        }
        try {
            return LocalDate.parse(rawDate.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static String normalizeLogoUrl(String logo) {
        if (!hasText(logo)) {
            return null;
        }
        String trimmed = logo.trim();
        if (trimmed.endsWith(".webp") || trimmed.endsWith(".png") || trimmed.endsWith(".jpg")) {
            return trimmed;
        }
        return trimmed + ".webp";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record PreparedSetData(
            String id,
            String name,
            String series,
            LocalDate releaseDate,
            Integer totalCards,
            String logoUrl,
            Instant syncedAt
    ) {
    }

    public record PreparedCardData(
            String id,
            String setId,
            String localId,
            int sortOrder,
            String name,
            String category,
            Integer hp,
            String stage,
            String evolveFrom,
            Integer retreat,
            String trainerType,
            String energyType,
            String rarity,
            String illustrator,
            String imageLowUrl,
            String imageHighUrl,
            Set<String> types,
            List<CardAttack> attacks,
            List<CardTypeModifier> weaknesses,
            List<CardTypeModifier> resistances,
            String effectText,
            Map<String, Object> rawPayload
    ) {
    }
}
