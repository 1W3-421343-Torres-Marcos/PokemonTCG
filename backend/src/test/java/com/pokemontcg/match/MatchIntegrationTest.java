package com.pokemontcg.match;

import com.pokemontcg.auth.domain.UserEntity;
import com.pokemontcg.auth.repository.UserRepository;
import com.pokemontcg.bot.BotDifficulty;
import com.pokemontcg.common.exception.ForbiddenException;
import com.pokemontcg.deck.domain.DeckEntity;
import com.pokemontcg.deck.repository.DeckRepository;
import com.pokemontcg.engine.dto.CardViewDTO;
import com.pokemontcg.engine.model.PlayerId;
import com.pokemontcg.engine.model.TurnPhase;
import com.pokemontcg.engine.model.WinReason;
import com.pokemontcg.match.domain.MatchStatus;
import com.pokemontcg.match.dto.CreateMatchRequest;
import com.pokemontcg.match.dto.MatchActionRequest;
import com.pokemontcg.match.dto.MatchActionType;
import com.pokemontcg.match.dto.MatchTurnResponse;
import com.pokemontcg.match.service.MatchService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class MatchIntegrationTest {

    @Autowired
    private MatchService matchService;

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsMatchJsonbStateRunsHumanAndBotTurnsAndUpdatesUserStats() {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setUsername("match_tester_" + UUID.randomUUID().toString().substring(0, 8));
        user.setEmail("match_" + UUID.randomUUID().toString().substring(0, 8) + "@test.local");
        user.setPasswordHash("hashed");
        user.setRole("USER");
        user.setMatchesPlayed(0);
        user.setMatchesWon(0);
        user.setCreatedAt(Instant.now());
        userRepository.saveAndFlush(user);

        List<DeckEntity> starters = deckRepository.findByStarterTrueOrderByNameAsc();
        assertThat(starters).hasSizeGreaterThanOrEqualTo(2);
        DeckEntity playerDeck = starters.get(0);
        DeckEntity cpuDeck = starters.get(1);

        MatchTurnResponse created = matchService.createMatch(
                user.getId(),
                new CreateMatchRequest(playerDeck.getId(), cpuDeck.getId(), BotDifficulty.BASIC)
        );
        assertThat(created.status()).isEqualTo(MatchStatus.IN_PROGRESS);
        assertThat(created.playerView().phase()).isEqualTo(TurnPhase.SETUP);
        assertThat(created.playerView().opponentSetupReady()).isTrue();
        assertThat(created.playerView().opponent().active()).isNotNull();

        // Force clear JPA persistence context to test real PostgreSQL JSONB deserialization!
        entityManager.flush();
        entityManager.clear();

        assertThat(matchService.getActiveMatch(user.getId())).isPresent();

        MatchTurnResponse loaded = matchService.getMatch(created.matchId(), user.getId());
        assertThat(loaded.playerView().myHand()).hasSize(created.playerView().myHand().size());

        // Enforce 403 for other users
        UUID stranger = UUID.randomUUID();
        assertThatThrownBy(() -> matchService.getMatch(created.matchId(), stranger))
                .isInstanceOf(ForbiddenException.class);

        // Select human setup Basic and advance until human MAIN_PHASE (running CPU Turn 1 automatically if CPU won flip)
        CardViewDTO firstBasic = loaded.playerView().myHand().stream()
                .filter(c -> "Pokemon".equalsIgnoreCase(c.category()) && "Basic".equalsIgnoreCase(c.stage()))
                .findFirst()
                .orElseThrow();

        MatchTurnResponse afterSetup = matchService.submitAction(
                created.matchId(),
                user.getId(),
                new MatchActionRequest(
                        MatchActionType.SELECT_SETUP,
                        firstBasic.instanceId(),
                        List.of(),
                        null, null, null, null, null, null, null, null
                )
        );
        assertThat(afterSetup.playerView().myActive()).isNotNull();
        assertThat(afterSetup.playerView().phase()).isEqualTo(TurnPhase.MAIN_PHASE);
        assertThat(afterSetup.playerView().activePlayer()).isEqualTo(PlayerId.PLAYER);

        // Human ends turn -> CPU executes its turn automatically -> returns to PLAYER MAIN_PHASE
        MatchTurnResponse afterTurn = matchService.submitAction(
                created.matchId(),
                user.getId(),
                new MatchActionRequest(MatchActionType.END_TURN, null, null, null, null, null, null, null, null, null, null)
        );
        assertThat(afterTurn.events()).isNotEmpty();
        assertThat(afterTurn.playerView().turnNumber()).isGreaterThan(afterSetup.playerView().turnNumber());

        // Concede match -> marks FINISHED and increments user matchesPlayed (lost)
        if (afterTurn.status() == MatchStatus.IN_PROGRESS) {
            MatchTurnResponse conceded = matchService.concedeMatch(created.matchId(), user.getId());
            assertThat(conceded.status()).isEqualTo(MatchStatus.FINISHED);
            assertThat(conceded.playerView().phase()).isEqualTo(TurnPhase.GAME_OVER);
            assertThat(conceded.playerView().winReason()).isEqualTo(WinReason.CONCEDED);
        }

        entityManager.flush();
        entityManager.clear();

        assertThat(matchService.getActiveMatch(user.getId())).isEmpty();
        UserEntity updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getMatchesPlayed()).isEqualTo(1);
    }
}
