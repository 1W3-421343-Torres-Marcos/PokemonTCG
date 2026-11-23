package com.pokemontcg.engine.effects;

import com.pokemontcg.engine.exception.IllegalGameActionException;
import com.pokemontcg.engine.model.ActiveModifier;
import com.pokemontcg.engine.model.AttackDefinition;
import com.pokemontcg.engine.model.CardInstance;
import com.pokemontcg.engine.model.GameState;
import com.pokemontcg.engine.model.ModifierType;
import com.pokemontcg.engine.model.PlayerId;
import com.pokemontcg.engine.model.PlayerState;
import com.pokemontcg.engine.model.PokemonInPlay;
import com.pokemontcg.engine.model.StatusCondition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Strategy registry mapping Base Set (base1) card IDs to reusable Trainer and Attack effects.
 */
public final class CardEffectRegistry {

    private static final CardEffectRegistry DEFAULT_INSTANCE = createDefaultRegistry();

    private final Map<String, TrainerEffect> trainers;
    private final Map<AttackKey, AttackEffect> attacks;

    private CardEffectRegistry(Map<String, TrainerEffect> trainers, Map<AttackKey, AttackEffect> attacks) {
        this.trainers = Map.copyOf(trainers);
        this.attacks = Map.copyOf(attacks);
    }

    public static CardEffectRegistry defaultRegistry() {
        return DEFAULT_INSTANCE;
    }

    public Optional<TrainerEffect> findTrainer(String cardDefId) {
        if (cardDefId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(trainers.get(cardDefId));
    }

    public Optional<AttackEffect> findAttack(String cardDefId, int attackIndex) {
        if (cardDefId == null || attackIndex < 0) {
            return Optional.empty();
        }
        return Optional.ofNullable(attacks.get(new AttackKey(cardDefId, attackIndex)));
    }

    public record AttackKey(String cardDefId, int attackIndex) {
    }

    private static CardEffectRegistry createDefaultRegistry() {
        Map<String, TrainerEffect> trainers = new HashMap<>();
        registerStarterTrainers(trainers);

        Map<AttackKey, AttackEffect> attacks = new HashMap<>();
        registerStarterAttacks(attacks);

        return new CardEffectRegistry(trainers, attacks);
    }

    private static void registerStarterTrainers(Map<String, TrainerEffect> trainers) {
        // base1-80 Defender: Attach to 1 of your Pokemon; reduces damage by 20 until end of opponent's next turn.
        trainers.put("base1-80", targetedTrainer(
                (state, actor, card) -> EffectContext.allPokemonInPlay(state.player(actor)).stream()
                        .map(PokemonInPlay::instanceId)
                        .toList(),
                (ctx, card, targetId) -> CombatBuffBlock.addModifierToPokemon(
                        ctx,
                        ctx.actor(),
                        targetId,
                        new ActiveModifier("Defender", ModifierType.DAMAGE_REDUCTION_20, 20, ctx.turnNumber() + 1)
                )
        ));

        // base1-81 Energy Retrieval: Trade 1 other card from hand for up to 2 basic Energy cards in discard.
        trainers.put("base1-81", targetedTrainer(
                (state, actor, trainerCard) -> {
                    PlayerState ps = state.player(actor);
                    boolean hasBasicEnergyInDiscard = ps.discardPile().stream()
                            .anyMatch(c -> c.definition().basicEnergy());
                    if (!hasBasicEnergyInDiscard) {
                        return List.of();
                    }
                    return ps.hand().stream()
                            .filter(c -> !c.instanceId().equals(trainerCard.instanceId()))
                            .map(CardInstance::instanceId)
                            .toList();
                },
                (ctx, card, targetHandId) -> {
                    DiscardEnergyBlock.retrieveBasicEnergiesFromDiscard(ctx, ctx.actor(), 2, "Energy Retrieval");
                    ctx.updatePlayer(ctx.actor(), ps -> {
                        List<CardInstance> nextHand = new ArrayList<>(ps.hand());
                        CardInstance traded = null;
                        for (int i = 0; i < nextHand.size(); i++) {
                            if (nextHand.get(i).instanceId().equals(targetHandId)) {
                                traded = nextHand.remove(i);
                                break;
                            }
                        }
                        if (traded == null) {
                            throw new IllegalGameActionException("La carta a descartar no está en tu mano");
                        }
                        List<CardInstance> nextDiscard = new ArrayList<>(ps.discardPile());
                        nextDiscard.add(traded);
                        return new PlayerState(
                                ps.deck(), nextHand, ps.active(), ps.bench(), ps.prizes(), nextDiscard,
                                ps.energyAttachedThisTurn(), ps.retreatedThisTurn(), ps.setupReady(),
                                ps.mulliganCount(), ps.plusPowerBonus()
                        );
                    });
                }
        ));

        // base1-84 PlusPower: +10 attack damage this turn.
        trainers.put("base1-84", instantTrainer(
                (state, actor) -> state.player(actor).active() != null,
                (ctx, card) -> CombatBuffBlock.addPlusPower(ctx, ctx.actor(), 10)
        ));

        // base1-88 Professor Oak: Discard your hand, then draw 7 cards.
        trainers.put("base1-88", instantTrainer(
                (state, actor) -> !state.player(actor).deck().isEmpty(),
                (ctx, card) -> {
                    DrawCardsBlock.discardHand(ctx, ctx.actor());
                    DrawCardsBlock.drawUpTo(ctx, ctx.actor(), 7, "Professor Oak");
                }
        ));

        // base1-90 Super Potion: Discard 1 Energy attached to 1 of your damaged Pokemon to heal up to 40 damage.
        trainers.put("base1-90", targetedTrainer(
                (state, actor, card) -> EffectContext.allPokemonInPlay(state.player(actor)).stream()
                        .filter(p -> p.damage() > 0 && !p.attachedEnergies().isEmpty())
                        .map(PokemonInPlay::instanceId)
                        .toList(),
                (ctx, card, targetId) -> {
                    DiscardEnergyBlock.discardFromPokemon(ctx, ctx.actor(), targetId, 1, null, "Super Potion");
                    HealBlock.healPokemon(ctx, ctx.actor(), targetId, 40, "Super Potion");
                }
        ));

        // base1-91 Bill: Draw 2 cards.
        trainers.put("base1-91", instantTrainer(
                (state, actor) -> !state.player(actor).deck().isEmpty(),
                (ctx, card) -> DrawCardsBlock.drawUpTo(ctx, ctx.actor(), 2, "Bill")
        ));

        // base1-92 Energy Removal: Choose 1 Energy attached to 1 opponent's Pokemon and discard it.
        trainers.put("base1-92", targetedTrainer(
                (state, actor, card) -> EffectContext.allPokemonInPlay(state.player(actor.opponent())).stream()
                        .filter(p -> !p.attachedEnergies().isEmpty())
                        .map(PokemonInPlay::instanceId)
                        .toList(),
                (ctx, card, targetId) -> DiscardEnergyBlock.discardFromPokemon(
                        ctx, ctx.actor().opponent(), targetId, 1, null, "Energy Removal"
                )
        ));

        // base1-93 Gust of Wind: Switch 1 of opponent's Benched Pokemon with their Active.
        trainers.put("base1-93", targetedTrainer(
                (state, actor, card) -> state.player(actor.opponent()).bench().stream()
                        .map(PokemonInPlay::instanceId)
                        .toList(),
                (ctx, card, targetId) -> SwitchActiveBlock.switchWithBench(
                        ctx, ctx.actor().opponent(), targetId, "Gust of Wind"
                )
        ));

        // base1-94 Potion: Heal up to 20 damage from 1 of your Pokemon.
        trainers.put("base1-94", targetedTrainer(
                (state, actor, card) -> EffectContext.allPokemonInPlay(state.player(actor)).stream()
                        .filter(p -> p.damage() > 0)
                        .map(PokemonInPlay::instanceId)
                        .toList(),
                (ctx, card, targetId) -> HealBlock.healPokemon(ctx, ctx.actor(), targetId, 20, "Potion")
        ));

        // base1-95 Switch: Switch 1 of your Benched Pokemon with your Active.
        trainers.put("base1-95", targetedTrainer(
                (state, actor, card) -> state.player(actor).bench().stream()
                        .map(PokemonInPlay::instanceId)
                        .toList(),
                (ctx, card, targetId) -> SwitchActiveBlock.switchWithBench(
                        ctx, ctx.actor(), targetId, "Switch"
                )
        ));
    }

    private static void registerStarterAttacks(Map<AttackKey, AttackEffect> attacks) {
        // base1-1 Alakazam #0: Confuse Ray (30 + coin heads Confused)
        attacks.put(new AttackKey("base1-1", 0), damagePlusHeadsStatus("Confuse Ray", StatusCondition.CONFUSED));

        // base1-2 Blastoise #0: Hydro Pump (40 + 10 per extra Water Energy, max +20)
        attacks.put(new AttackKey("base1-2", 0), ctx -> {
            int bonus = extraWaterEnergyBonus(ctx.attack(), ctx.attacker());
            int base = ctx.attack().baseDamage() > 0 ? ctx.attack().baseDamage() : 40;
            ctx.dealCalculatedDamage(base + bonus);
        });

        // base1-3 Chansey #0 Scrunch, #1 Double-edge
        attacks.put(new AttackKey("base1-3", 0), headsImmuneShield("Scrunch"));
        attacks.put(new AttackKey("base1-3", 1), damagePlusRecoil("Double-edge", 80));

        // base1-4 Charizard #0: Fire Spin (100, discard 2 energy cards)
        attacks.put(new AttackKey("base1-4", 0), damagePlusDiscardAttackerEnergy("Fire Spin", 2, null));

        // base1-10 Mewtwo #0 Psychic, #1 Barrier
        attacks.put(new AttackKey("base1-10", 0), ctx -> {
            PokemonInPlay defender = ctx.defender();
            int attachedCount = defender == null ? 0 : defender.attachedEnergies().size();
            int base = Math.max(10, ctx.attack().baseDamage());
            ctx.dealCalculatedDamage(base + 10 * attachedCount);
        });
        attacks.put(new AttackKey("base1-10", 1), ctx -> {
            EffectContext fx = ctx.effects();
            DiscardEnergyBlock.discardFromPokemon(fx, fx.actor(), ctx.attacker().instanceId(), 1, "Psychic", "Barrier");
            addSelfShield(fx, "Barrier", ModifierType.IMMUNE_NEXT_TURN);
        });

        // base1-14 Raichu #0 Agility, #1 Thunder
        attacks.put(new AttackKey("base1-14", 0), ctx -> {
            ctx.dealCalculatedDamage(ctx.attack().baseDamage());
            CoinFlipConditionalBlock.onHeads(
                    ctx.effects(), ctx.effects().actor(), "AGILITY",
                    "Raichu lanzó moneda por Agility: ",
                    () -> addSelfShield(ctx.effects(), "Agility", ModifierType.IMMUNE_NEXT_TURN)
            );
        });
        attacks.put(new AttackKey("base1-14", 1), damagePlusTailsRecoil("Raichu - Thunder", 30));

        // base1-16 Zapdos #0 Thunder, #1 Thunderbolt
        attacks.put(new AttackKey("base1-16", 0), damagePlusTailsRecoil("Zapdos - Thunder", 30));
        attacks.put(new AttackKey("base1-16", 1), ctx -> {
            ctx.dealCalculatedDamage(ctx.attack().baseDamage());
            DiscardEnergyBlock.discardAllFromAttacker(ctx.effects(), "Thunderbolt");
        });

        // base1-20 Electabuzz #0 Thundershock, #1 Thunderpunch
        attacks.put(new AttackKey("base1-20", 0), damagePlusHeadsStatus("Thundershock", StatusCondition.PARALYZED));
        attacks.put(new AttackKey("base1-20", 1), ctx -> {
            EffectContext fx = ctx.effects();
            boolean heads = fx.flipCoin(fx.actor(), "THUNDERPUNCH", "Electabuzz lanzó moneda por Thunderpunch: ");
            int base = ctx.attack().baseDamage() > 0 ? ctx.attack().baseDamage() : 30;
            if (heads) {
                ctx.dealCalculatedDamage(base + 10);
            } else {
                ctx.dealCalculatedDamage(base);
                DamageBlock.dealSelfDamage(fx, fx.actor(), 10, "Thunderpunch");
            }
        });

        // base1-23 Arcanine #0 Flamethrower, #1 Take Down
        attacks.put(new AttackKey("base1-23", 0), damagePlusDiscardAttackerEnergy("Flamethrower", 1, "Fire"));
        attacks.put(new AttackKey("base1-23", 1), damagePlusRecoil("Take Down", 30));

        // base1-24 Charmeleon #1 Flamethrower
        attacks.put(new AttackKey("base1-24", 1), damagePlusDiscardAttackerEnergy("Flamethrower", 1, "Fire"));

        // base1-30 Ivysaur #1 Poisonpowder
        attacks.put(new AttackKey("base1-30", 1), damagePlusDirectStatus(StatusCondition.POISONED));

        // base1-32 Kadabra #0 Recover
        attacks.put(new AttackKey("base1-32", 0), recoverAttack("Psychic", "Recover"));

        // base1-34 Machoke #0 Karate Chop, #1 Submission
        attacks.put(new AttackKey("base1-34", 0), ctx -> {
            int base = ctx.attack().baseDamage() > 0 ? ctx.attack().baseDamage() : 50;
            int adjusted = Math.max(0, base - ctx.attacker().damage());
            ctx.dealCalculatedDamage(adjusted);
        });
        attacks.put(new AttackKey("base1-34", 1), damagePlusRecoil("Submission", 20));

        // base1-36 Magmar #1 Flamethrower
        attacks.put(new AttackKey("base1-36", 1), damagePlusDiscardAttackerEnergy("Flamethrower", 1, "Fire"));

        // base1-42 Wartortle #0 Withdraw
        attacks.put(new AttackKey("base1-42", 0), headsImmuneShield("Withdraw"));

        // base1-43 Abra #0 Psyshock
        attacks.put(new AttackKey("base1-43", 0), damagePlusHeadsStatus("Psyshock", StatusCondition.PARALYZED));

        // base1-44 Bulbasaur #0 Leech Seed
        attacks.put(new AttackKey("base1-44", 0), ctx -> {
            int dealt = ctx.dealCalculatedDamage(ctx.attack().baseDamage());
            if (dealt > 0) {
                HealBlock.healPokemon(
                        ctx.effects(), ctx.effects().actor(), ctx.attacker().instanceId(), 10, "Leech Seed"
                );
            }
        });

        // base1-46 Charmander #1 Ember
        attacks.put(new AttackKey("base1-46", 1), damagePlusDiscardAttackerEnergy("Ember", 1, "Fire"));

        // base1-56 Onix #1 Harden
        attacks.put(new AttackKey("base1-56", 1),
                ctx -> addSelfShield(ctx.effects(), "Harden", ModifierType.PREVENT_30_OR_LESS_NEXT_TURN));

        // base1-58 Pikachu #1 Thunder Jolt
        attacks.put(new AttackKey("base1-58", 1), damagePlusTailsRecoil("Thunder Jolt", 10));

        // base1-63 Squirtle #0 Bubble, #1 Withdraw
        attacks.put(new AttackKey("base1-63", 0), damagePlusHeadsStatus("Bubble", StatusCondition.PARALYZED));
        attacks.put(new AttackKey("base1-63", 1), headsImmuneShield("Withdraw"));

        // base1-64 Starmie #0 Recover, #1 Star Freeze
        attacks.put(new AttackKey("base1-64", 0), recoverAttack("Water", "Recover"));
        attacks.put(new AttackKey("base1-64", 1), damagePlusHeadsStatus("Star Freeze", StatusCondition.PARALYZED));

        // base1-66 Tangela #0 Bind, #1 Poisonpowder
        attacks.put(new AttackKey("base1-66", 0), damagePlusHeadsStatus("Bind", StatusCondition.PARALYZED));
        attacks.put(new AttackKey("base1-66", 1), damagePlusDirectStatus(StatusCondition.POISONED));
    }

    private static TrainerEffect targetedTrainer(TargetSelector selector, TargetExecutor executor) {
        return new TrainerEffect() {
            @Override
            public List<String> validTargetIds(GameState state, PlayerId actor, CardInstance trainerCard) {
                return selector.select(state, actor, trainerCard);
            }

            @Override
            public void execute(EffectContext ctx, CardInstance trainerCard, String targetId) {
                List<String> validIds = validTargetIds(ctx.state(), ctx.actor(), trainerCard);
                if (targetId == null || targetId.isBlank() || !validIds.contains(targetId)) {
                    throw new IllegalGameActionException(
                            "Objetivo inválido para " + trainerCard.definition().name()
                    );
                }
                executor.run(ctx, trainerCard, targetId);
            }
        };
    }

    private static TrainerEffect instantTrainer(PlayPredicate canPlay, InstantExecutor executor) {
        return new TrainerEffect() {
            @Override
            public List<String> validTargetIds(GameState state, PlayerId actor, CardInstance trainerCard) {
                return List.of();
            }

            @Override
            public boolean canPlay(GameState state, PlayerId actor, CardInstance trainerCard) {
                return canPlay.test(state, actor);
            }

            @Override
            public void execute(EffectContext ctx, CardInstance trainerCard, String targetId) {
                if (!canPlay(ctx.state(), ctx.actor(), trainerCard)) {
                    throw new IllegalGameActionException(
                            "No se cumplen las condiciones para jugar " + trainerCard.definition().name()
                    );
                }
                executor.run(ctx, trainerCard);
            }
        };
    }

    private static AttackEffect damagePlusHeadsStatus(String attackName, StatusCondition condition) {
        return ctx -> {
            ctx.dealCalculatedDamage(ctx.attack().baseDamage());
            CoinFlipConditionalBlock.onHeads(
                    ctx.effects(), ctx.effects().actor(), attackName.toUpperCase(),
                    attackName + " lanzó moneda: ",
                    () -> ApplyStatusBlock.applyToDefenderActive(ctx, condition)
            );
        };
    }

    private static AttackEffect damagePlusDirectStatus(StatusCondition condition) {
        return ctx -> {
            ctx.dealCalculatedDamage(ctx.attack().baseDamage());
            ApplyStatusBlock.applyToDefenderActive(ctx, condition);
        };
    }

    private static AttackEffect damagePlusRecoil(String attackName, int recoil) {
        return ctx -> {
            ctx.dealCalculatedDamage(ctx.attack().baseDamage());
            DamageBlock.dealSelfDamage(ctx.effects(), ctx.effects().actor(), recoil, attackName);
        };
    }

    private static AttackEffect damagePlusTailsRecoil(String attackName, int recoil) {
        return ctx -> {
            ctx.dealCalculatedDamage(ctx.attack().baseDamage());
            CoinFlipConditionalBlock.onTails(
                    ctx.effects(), ctx.effects().actor(), attackName.toUpperCase(),
                    attackName + " lanzó moneda: ",
                    () -> DamageBlock.dealSelfDamage(ctx.effects(), ctx.effects().actor(), recoil, attackName)
            );
        };
    }

    private static AttackEffect damagePlusDiscardAttackerEnergy(String attackName, int count, String preferredType) {
        return ctx -> {
            ctx.dealCalculatedDamage(ctx.attack().baseDamage());
            DiscardEnergyBlock.discardFromPokemon(
                    ctx.effects(), ctx.effects().actor(), ctx.attacker().instanceId(), count, preferredType, attackName
            );
        };
    }

    private static AttackEffect headsImmuneShield(String attackName) {
        return ctx -> CoinFlipConditionalBlock.onHeads(
                ctx.effects(), ctx.effects().actor(), attackName.toUpperCase(),
                attackName + " lanzó moneda: ",
                () -> addSelfShield(ctx.effects(), attackName, ModifierType.IMMUNE_NEXT_TURN)
        );
    }

    private static AttackEffect recoverAttack(String preferredType, String attackName) {
        return ctx -> {
            EffectContext fx = ctx.effects();
            DiscardEnergyBlock.discardFromPokemon(fx, fx.actor(), ctx.attacker().instanceId(), 1, preferredType, attackName);
            HealBlock.healAllActive(fx, fx.actor(), attackName);
        };
    }

    private static void addSelfShield(EffectContext fx, String sourceName, ModifierType modifierType) {
        PokemonInPlay active = fx.state().player(fx.actor()).active();
        if (active == null) {
            return;
        }
        CombatBuffBlock.addModifierToPokemon(
                fx,
                fx.actor(),
                active.instanceId(),
                new ActiveModifier(sourceName, modifierType, 0, fx.turnNumber() + 1)
        );
    }

    private static int extraWaterEnergyBonus(AttackDefinition attack, PokemonInPlay attacker) {
        if (attacker == null || attack == null) {
            return 0;
        }
        int requiredWater = 0;
        int requiredColorless = 0;
        for (String req : attack.cost()) {
            if ("Water".equalsIgnoreCase(req)) {
                requiredWater++;
            } else if ("Colorless".equalsIgnoreCase(req)) {
                requiredColorless++;
            }
        }
        int waterUnits = 0;
        int otherUnits = 0;
        for (CardInstance energy : attacker.attachedEnergies()) {
            int units = Math.max(1, energy.definition().providedEnergyCount());
            String type = Objects.requireNonNullElse(
                    energy.definition().providedEnergyType(),
                    energy.definition().primaryType()
            );
            if ("Water".equalsIgnoreCase(type)) {
                waterUnits += units;
            } else {
                otherUnits += units;
            }
        }
        int remainingColorless = Math.max(0, requiredColorless - otherUnits);
        int extraWater = waterUnits - requiredWater - remainingColorless;
        if (extraWater <= 0) {
            return 0;
        }
        return Math.min(2, extraWater) * 10;
    }

    @FunctionalInterface
    private interface TargetSelector {
        List<String> select(GameState state, PlayerId actor, CardInstance trainerCard);
    }

    @FunctionalInterface
    private interface TargetExecutor {
        void run(EffectContext ctx, CardInstance trainerCard, String targetId);
    }

    @FunctionalInterface
    private interface PlayPredicate {
        boolean test(GameState state, PlayerId actor);
    }

    @FunctionalInterface
    private interface InstantExecutor {
        void run(EffectContext ctx, CardInstance trainerCard);
    }
}
