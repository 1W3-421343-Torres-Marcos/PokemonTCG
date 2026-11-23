---
name: web-battle-board
description: Usar al desarrollar el Setup de Partida (/play/setup), el Tablero de Combate (/play/:matchId), el HUD con Signals y la reproducción secuencial de GameEvent.
---

# Módulo: `web-battle-board`

## Alcance
Flujo completo de combate PvE en Angular 18+: configuración de partida contra la CPU, renderizado reactivo del tablero sobre `PlayerViewDTO`, controles de acción y reproducción secuencial animada de `List<GameEvent>`.

## Especificaciones técnicas
- **Setup de Partida (`/play/setup`)**: selector de mazo del jugador (solo mazos propios con `isValid = true` o *starter decks*), selector de mazo de la CPU (`cpuEligible = true`) y selector de dificultad del Bot (`BASIC` | `TACTICAL`); envía `POST /api/matches` y navega a `/play/:matchId`.
- **Tablero de Combate (`/play/:matchId`)**:
  - **Zona Rival (superior)**: contador de mazo, pila de descarte inspeccionable (`DiscardPileModal`), 6 premios boca abajo, banca (hasta 5 slots), Pokémon Activo (barra de PS `HpBar`, energías adheridas `EnergyBadge`, íconos de estado alterado `StatusBadge`) y mano rival oculta (dorso + contador).
  - **Zona Jugador (inferior)**: Pokémon Activo (`HpBar`, energías adheridas, `StatusBadge`), banca (hasta 5 slots interactivos), premios, mazo, descarte inspeccionable y mano interactiva con **resaltado de cartas jugables** según la fase actual (`SETUP`, `MAIN_PHASE`, promoción obligatoria).
  - **HUD y Controles**: indicador de turno (`Tu turno` / `Turno CPU`) y fase actual, menú de ataques del Activo (habilitados solo si cumplen el costo de energía adherida y las condiciones del turno/estado), botones de **Retirar**, **Terminar Turno** y **Rendirse**, modal de inspección ampliada de cualquier carta visible (`CardDetailModal`), **panel de Historial de Combate (`EventLogFeed`)** y **modal de Victoria/Derrota (`MatchResultModal`)**.
- **Signals y Animador (`MatchStore` + `EventAnimatorService`)**:
  - Al recibir `MatchTurnResponse(matchId, PlayerViewDTO, List<GameEvent>)`, `EventAnimatorService` encola y reproduce secuencialmente cada `GameEvent` (jugada humana + pasos del Bot) con breve pausa visual (`450–700ms` por evento, selector `1x`/`2x` y botón *Saltar*), bloqueando controles del usuario mientras dura la reproducción.

## Reglas de negocio
- **Cliente tonto estricto**: prohibido calcular daño, debilidades, lanzamientos de moneda o decisiones del Bot en TypeScript; solo habilitar/resaltar acciones coherentes con `PlayerViewDTO` y enviar `GameAction` a `POST /api/matches/{id}/actions`.
- **Viewport sin scroll**: el tablero en desktop (`≥1280×720`) debe encajar en `100dvh` sin scroll vertical de página, siguiendo `docs/design.md`.

## Archivos que toca
- `frontend/src/app/features/battle/**`
- `frontend/src/app/shared/ui/**`

## Criterios de aceptación
- [x] Setup `/play/setup` y Tablero `/play/:matchId` con Zona Rival, Zona Jugador, HUD de turno/fase/ataques, descarte inspeccionable, Historial de Combate secuencial y modal Victoria/Derrota operativos en F3.
- [x] Acciones completas (`EVOLVE`, `RETREAT`, `PLAY_TRAINER`), resaltado de cartas jugables en mano, íconos de estado alterado y animaciones secuenciales de `List<GameEvent>` operativos en F4.
