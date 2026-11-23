---
name: game-engine
description: Usar al diseñar o modificar las reglas puras de Pokémon TCG, la máquina de estados de turno, cálculo de daño, estados alterados, eventos o el Registry de efectos.
---

# Módulo: `game-engine`

## Alcance
Núcleo de reglas oficiales de Pokémon TCG en **Java 21 puro**, determinista y desacoplado de Spring, JPA y HTTP para testear partidas completas en milisegundos con JUnit 5 y habilitar PvP futuro.

## Especificaciones técnicas
- **Restricción de paquete (`com.pokemontcg.engine`)**: prohibido importar `org.springframework.*`, `jakarta.persistence.*` o clases de `match`/`catalog`. Recibe definiciones mediante `CardDefinition` inmutable.
- **Modelos principales**:
  - `GameState`, `PlayerState` (mazo, mano, descarte, 6 premios, activo, banca hasta 5, flags de turno), `PokemonInPlay` (carta, daño acumulado, energías adjuntas, condiciones de estado, turno de entrada/evolución).
  - `GameAction` (sealed interface): `SelectSetupAction`, `PlayBasicAction`, `AttachEnergyAction`, `EvolveAction`, `PlayTrainerAction`, `RetreatAction`, `AttackAction`, `PromoteActiveAction`, `EndTurnAction`, `ConcedeAction`.
  - `GameEvent` (sealed interface): `GameStartedEvent`, `MulliganEvent`, `TurnStartedEvent`, `CardDrawnEvent`, `PokemonPlayedEvent`, `EnergyAttachedEvent`, `PokemonEvolvedEvent`, `TrainerPlayedEvent`, `AttackDeclaredEvent`, `CoinFlippedEvent`, `DamageDealtEvent`, `StatusAppliedEvent`, `CheckupTickEvent`, `PokemonKnockedOutEvent`, `PrizeTakenEvent`, `PokemonRetreatedEvent`, `GameEndedEvent`.
  - `PlayerViewDTO`: proyección segura para un jugador (su mano completa, zonas visibles de ambos y solo tamaños `opponentHandCount`, `opponentDeckCount`, `opponentPrizesRemaining` del rival).
- **Máquina de estados del turno (`TurnPhase`)**:
  - `SETUP -> DRAW_PHASE -> MAIN_PHASE -> ATTACK_PHASE -> POKEMON_CHECKUP -> GAME_OVER` (más subestado explícito cuando un KO exige promoción de banca).
  - `GameEngine.applyAction(GameState, PlayerId, GameAction)` valida turno y fase actual; ante ilegalidad lanza `IllegalGameActionException`.
- **Registry de efectos (`CardEffectRegistry`)**:
  - `Map<String, CardEffect>` indexado por ID TCGdex de carta o ataque, compuesto por bloques reutilizables: `DamageBlock`, `DrawCardsBlock`, `HealBlock`, `ApplyStatusBlock`, `SwitchActiveBlock`, `DiscardEnergyBlock`, `CoinFlipConditionalBlock`.
- **Determinismo**: `RandomProvider` inyectable (implementación real y mock/fijo para JUnit 5) usado en barajado y lanzamientos de moneda.

## Reglas de negocio
- **Setup y Mulligan**: mano inicial de 7 cartas; si no hay Básico, revelar mano, rebarajar y robar 7 (el rival roba 1 carta extra opcional); colocar 1 Activo, hasta 5 en Banca y 6 Premios.
- **Límites por turno**: en Turno 1 del primer jugador no se puede atacar ni evolucionar; máximo 1 Energía adjunta y 1 Retirada por turno; no evolucionar un Pokémon bajado o evolucionado en el mismo turno.
- **Cálculo de daño**: daño base + modificadores de efecto -> Debilidad (`×2`) -> Resistencia (`-30`, piso `0`).
- **Estados alterados**: `POISONED` (10 en Checkup), `BURNED` (20 + moneda para curar), `ASLEEP` (no ataca/retira, moneda en Checkup), `PARALYZED` (no ataca/retira, se cura al fin de tu turno), `CONFUSED` (moneda al atacar; cruz = 30 daño propio). `ASLEEP`, `PARALYZED` y `CONFUSED` son mutuamente excluyentes; evolucionar o mover a banca limpia todos los estados.
- **Condiciones de victoria**: tomar los 6 Premios, dejar al rival sin Pokémon en juego tras un KO o que el rival no pueda robar al inicio de su `DRAW_PHASE` (deck-out).

## Archivos que toca
- `backend/src/main/java/com/pokemontcg/engine/**`
- `backend/src/test/java/com/pokemontcg/engine/**`

## Criterios de aceptación
- [x] Cero imports de Spring/JPA en `com.pokemontcg.engine` y proyección `PlayerViewDTO` verificada sin fugas de mano/mazo rival.
- [x] Flujo núcleo (F3: setup, robo, básicos, energía, ataque directo, KO, premios, victoria) probado en JUnit 5.
- [ ] Reglas completas (F4: evolución, retirada, debilidad/resistencia, 5 estados alterados y Checkup determinista) probadas en JUnit 5.
- [ ] `CardEffectRegistry` operativo con bloques componibles cubriendo cartas clave de `base1` sin condicionales monolíticos.
