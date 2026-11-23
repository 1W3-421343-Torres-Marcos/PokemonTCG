package com.pokemontcg.engine;

import com.pokemontcg.engine.dto.PlayerViewDTO;
import com.pokemontcg.engine.dto.PlayerViewProjector;
import com.pokemontcg.engine.model.CardInstance;
import com.pokemontcg.engine.model.GameState;
import com.pokemontcg.engine.model.PlayerId;
import com.pokemontcg.engine.model.PlayerState;
import com.pokemontcg.engine.model.PokemonInPlay;
import com.pokemontcg.engine.model.TurnPhase;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerViewProjectorTest {

    @Test
    void computesAuthoritativeCanPayAndCanAttackFlags() {
        CardInstance playerActiveCard = new CardInstance("p-1", EngineTestFixtures.charmander());
        PokemonInPlay playerActive = new PokemonInPlay(
                playerActiveCard.instanceId(),
                playerActiveCard,
                List.of(playerActiveCard),
                List.of(new CardInstance("p-e1", EngineTestFixtures.fireEnergy())),
                0,
                Set.of(),
                0,
                0
        );

        CardInstance cpuActiveCard = new CardInstance("c-1", EngineTestFixtures.squirtle());
        PokemonInPlay cpuActive = PokemonInPlay.fromBasic(cpuActiveCard, 0);

        PlayerState playerState = new PlayerState(
                List.of(),
                List.of(new CardInstance("p-h1", EngineTestFixtures.waterEnergy())),
                playerActive,
                List.of(),
                List.of(),
                List.of(),
                false,
                false,
                true,
                0
        );
        PlayerState cpuState = new PlayerState(
                List.of(),
                List.of(),
                cpuActive,
                List.of(),
                List.of(),
                List.of(),
                false,
                false,
                true,
                0
        );

        GameState turn1State = new GameState(
                1,
                PlayerId.PLAYER,
                PlayerId.PLAYER,
                TurnPhase.MAIN_PHASE,
                null,
                Map.of(PlayerId.PLAYER, playerState, PlayerId.CPU, cpuState),
                null,
                null,
                1L
        );

        PlayerViewDTO turn1View = PlayerViewProjector.project(turn1State, PlayerId.PLAYER);
        // Attack index 0 ("Scratch", [Colorless]) is payable with 1 Fire Energy,
        // Attack index 1 ("Ember", [Fire, Colorless]) is NOT payable yet.
        assertThat(turn1View.myActive().attacks().get(0).canPay()).isTrue();
        assertThat(turn1View.myActive().attacks().get(1).canPay()).isFalse();
        // Turn 1 starting player cannot attack even if an attack cost is paid.
        assertThat(turn1View.canAttackThisTurn()).isFalse();

        GameState turn3State = new GameState(
                3,
                PlayerId.PLAYER,
                PlayerId.PLAYER,
                TurnPhase.MAIN_PHASE,
                null,
                Map.of(PlayerId.PLAYER, playerState, PlayerId.CPU, cpuState),
                null,
                null,
                1L
        );

        PlayerViewDTO turn3View = PlayerViewProjector.project(turn3State, PlayerId.PLAYER);
        assertThat(turn3View.canAttackThisTurn()).isTrue();
    }

    @Test
    void doesNotLeakOpponentHandDeckOrPrizeIdentities() {
        PokemonInPlay playerActive = PokemonInPlay.fromBasic(new CardInstance("p-1", EngineTestFixtures.charmander()), 1);
        PokemonInPlay cpuActive = PokemonInPlay.fromBasic(new CardInstance("c-1", EngineTestFixtures.squirtle()), 1);

        PlayerState playerState = new PlayerState(
                List.of(new CardInstance("p-d1", EngineTestFixtures.fireEnergy())),
                List.of(new CardInstance("p-h1", EngineTestFixtures.charmander())),
                playerActive,
                List.of(),
                List.of(new CardInstance("p-pr1", EngineTestFixtures.fireEnergy())),
                List.of(),
                false,
                false,
                true,
                0
        );

        PlayerState cpuSecretA = new PlayerState(
                List.of(new CardInstance("c-d1", EngineTestFixtures.waterEnergy())),
                List.of(new CardInstance("c-h1", EngineTestFixtures.squirtle()), new CardInstance("c-h2", EngineTestFixtures.waterEnergy())),
                cpuActive,
                List.of(),
                List.of(new CardInstance("c-pr1", EngineTestFixtures.squirtle())),
                List.of(),
                false,
                false,
                true,
                0
        );

        PlayerState cpuSecretB = new PlayerState(
                List.of(new CardInstance("c-d1", EngineTestFixtures.magmar())),
                List.of(new CardInstance("c-h1", EngineTestFixtures.magmar()), new CardInstance("c-h2", EngineTestFixtures.doubleColorlessEnergy())),
                cpuActive,
                List.of(),
                List.of(new CardInstance("c-pr1", EngineTestFixtures.doubleColorlessEnergy())),
                List.of(),
                false,
                false,
                true,
                0
        );

        GameState stateA = new GameState(2, PlayerId.PLAYER, PlayerId.PLAYER, TurnPhase.MAIN_PHASE, null,
                Map.of(PlayerId.PLAYER, playerState, PlayerId.CPU, cpuSecretA), null, null, 1L);
        GameState stateB = new GameState(2, PlayerId.PLAYER, PlayerId.PLAYER, TurnPhase.MAIN_PHASE, null,
                Map.of(PlayerId.PLAYER, playerState, PlayerId.CPU, cpuSecretB), null, null, 1L);

        PlayerViewDTO viewA = PlayerViewProjector.project(stateA, PlayerId.PLAYER);
        PlayerViewDTO viewB = PlayerViewProjector.project(stateB, PlayerId.PLAYER);

        assertThat(viewA).isEqualTo(viewB);
        assertThat(viewA.opponent().handCount()).isEqualTo(2);
        assertThat(viewA.opponent().deckCount()).isEqualTo(1);
        assertThat(viewA.opponent().prizesRemaining()).isEqualTo(1);
    }
}
