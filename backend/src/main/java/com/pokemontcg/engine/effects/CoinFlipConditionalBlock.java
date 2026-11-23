package com.pokemontcg.engine.effects;

import com.pokemontcg.engine.model.PlayerId;

/**
 * Composable block for coin-flip conditional side effects.
 */
public final class CoinFlipConditionalBlock {

    private CoinFlipConditionalBlock() {
    }

    public static void onHeads(
            EffectContext ctx,
            PlayerId actor,
            String reason,
            String descriptionPrefix,
            Runnable onHeadsAction
    ) {
        if (ctx.flipCoin(actor, reason, descriptionPrefix)) {
            onHeadsAction.run();
        }
    }

    public static void onTails(
            EffectContext ctx,
            PlayerId actor,
            String reason,
            String descriptionPrefix,
            Runnable onTailsAction
    ) {
        if (!ctx.flipCoin(actor, reason, descriptionPrefix)) {
            onTailsAction.run();
        }
    }
}
