package com.pokemontcg.engine.effects;

import com.pokemontcg.engine.event.GameEventType;
import com.pokemontcg.engine.model.CardInstance;
import com.pokemontcg.engine.model.PlayerId;
import com.pokemontcg.engine.model.PlayerState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Composable block for drawing cards and discarding the hand (e.g., Bill, Professor Oak).
 */
public final class DrawCardsBlock {

    private DrawCardsBlock() {
    }

    public static int drawUpTo(EffectContext ctx, PlayerId owner, int count, String reason) {
        if (count <= 0) {
            return 0;
        }
        PlayerState ps = ctx.state().player(owner);
        int toDraw = Math.min(count, ps.deck().size());
        if (toDraw == 0) {
            return 0;
        }

        List<CardInstance> nextDeck = new ArrayList<>(ps.deck());
        List<CardInstance> nextHand = new ArrayList<>(ps.hand());
        for (int i = 0; i < toDraw; i++) {
            nextHand.add(nextDeck.remove(0));
        }

        ctx.updatePlayer(owner, current -> new PlayerState(
                nextDeck, nextHand, current.active(), current.bench(), current.prizes(), current.discardPile(),
                current.energyAttachedThisTurn(), current.retreatedThisTurn(), current.setupReady(),
                current.mulliganCount(), current.plusPowerBonus()
        ));

        ctx.emit(
                GameEventType.CARD_DRAWN,
                owner,
                ctx.displayPlayer(owner) + " robó " + toDraw + " carta(s) (" + reason + ").",
                Map.of("reason", reason, "count", toDraw, "deckRemaining", nextDeck.size())
        );
        return toDraw;
    }

    public static int discardHand(EffectContext ctx, PlayerId owner) {
        PlayerState ps = ctx.state().player(owner);
        int discarded = ps.hand().size();
        if (discarded == 0) {
            return 0;
        }
        List<CardInstance> nextDiscard = new ArrayList<>(ps.discardPile());
        nextDiscard.addAll(ps.hand());
        ctx.updatePlayer(owner, current -> new PlayerState(
                current.deck(), List.of(), current.active(), current.bench(), current.prizes(), nextDiscard,
                current.energyAttachedThisTurn(), current.retreatedThisTurn(), current.setupReady(),
                current.mulliganCount(), current.plusPowerBonus()
        ));
        return discarded;
    }
}
