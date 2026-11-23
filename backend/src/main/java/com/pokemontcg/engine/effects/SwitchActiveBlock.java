package com.pokemontcg.engine.effects;

import com.pokemontcg.engine.event.GameEventType;
import com.pokemontcg.engine.exception.IllegalGameActionException;
import com.pokemontcg.engine.model.PlayerId;
import com.pokemontcg.engine.model.PlayerState;
import com.pokemontcg.engine.model.PokemonInPlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Composable block for swapping an Active Pokémon with a Benched Pokémon (Switch, Gust of Wind).
 */
public final class SwitchActiveBlock {

    private SwitchActiveBlock() {
    }

    public static void switchWithBench(
            EffectContext ctx,
            PlayerId owner,
            String benchInstanceId,
            String sourceName
    ) {
        PlayerState ps = ctx.state().player(owner);
        PokemonInPlay currentActive = ps.active();
        if (currentActive == null) {
            throw new IllegalGameActionException("No hay Pokémon Activo para intercambiar");
        }
        List<PokemonInPlay> nextBench = new ArrayList<>(ps.bench());
        int benchIndex = -1;
        for (int i = 0; i < nextBench.size(); i++) {
            if (nextBench.get(i).instanceId().equals(benchInstanceId)) {
                benchIndex = i;
                break;
            }
        }
        if (benchIndex < 0) {
            throw new IllegalGameActionException("El Pokémon seleccionado no está en la Banca");
        }

        PokemonInPlay promoted = nextBench.get(benchIndex);
        PokemonInPlay benchedOldActive = currentActive.withClearedCombatState();
        nextBench.set(benchIndex, benchedOldActive);

        ctx.updatePlayer(owner, current -> new PlayerState(
                current.deck(),
                current.hand(),
                promoted,
                nextBench,
                current.prizes(),
                current.discardPile(),
                current.energyAttachedThisTurn(),
                current.retreatedThisTurn(),
                current.setupReady(),
                current.mulliganCount(),
                current.plusPowerBonus()
        ));

        ctx.emit(
                GameEventType.ACTIVE_PROMOTED,
                owner,
                sourceName + ": " + promoted.activeCard().definition().name()
                        + " pasó al puesto Activo por " + currentActive.activeCard().definition().name() + ".",
                Map.of(
                        "source", sourceName,
                        "promotedName", promoted.activeCard().definition().name(),
                        "previousName", currentActive.activeCard().definition().name()
                )
        );
    }
}
