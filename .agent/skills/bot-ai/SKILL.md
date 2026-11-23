---
name: bot-ai
description: Usar al implementar o mejorar la IA oponente CPU, sus estrategias por dificultad y las simulaciones Bot vs. Bot en JUnit 5.
---

# Módulo: `bot-ai`

## Alcance
Oponente CPU basado en heurísticas en **Java 21 puro**, justo por diseño (solo observa su `PlayerViewDTO`) y ejecutable en tests de simulación sin Spring ni base de datos.

## Especificaciones técnicas
- **Restricción de paquete (`com.pokemontcg.bot`)**: depende únicamente de `com.pokemontcg.engine`; cero imports de Spring/JPA.
- **Patrón Strategy**:
  - Interfaz `BotStrategy`: `GameAction decideNextAction(PlayerViewDTO botView)`.
  - Enum `BotDifficulty`: `BASIC`, `TACTICAL`.
  - `BasicBotStrategy` (F3):
    - En `SETUP` / promoción: elige el Pokémon Básico con mayor HP disponible.
    - En `MAIN_PHASE`: baja Básicos si hay espacio en banca, adjunta energía al Activo (priorizando el ataque usable más cercano), ejecuta el ataque legal de mayor daño base o envía `EndTurnAction`.
  - `TacticalBotStrategy` (F4):
    - Prioriza Entrenadores de robo/curación, evoluciona líneas listas cuando aporta ventaja, evalúa daño efectivo (incluyendo debilidad `×2` y resistencia `-30` frente al Activo rival), ejecuta retirada táctica si el Activo está en riesgo de KO y tiene banca cargada, y selecciona ataques para asegurar KO o aplicar estados útiles.
- **Orquestador de turno (`BotTurnRunner`)**:
  - Bucle: proyecta `PlayerViewDTO` del Bot -> consulta `BotStrategy.decideNextAction` -> ejecuta `GameEngine.applyAction` -> acumula `GameEvent` hasta que termina el turno del Bot o la partida pasa a `GAME_OVER`.
  - Guardia de seguridad: límite `MAX_ACTIONS_PER_TURN = 25`; si se alcanza, fuerza `EndTurnAction` para impedir bucles infinitos.

## Reglas de negocio
- **Bot justo**: el Bot jamás inspecciona `GameState`, la mano del humano ni el orden del mazo.
- **Legalidad garantizada**: todas las jugadas del Bot pasan por la misma máquina de estados (`GameEngine.applyAction`) que las jugadas humanas.

## Archivos que toca
- `backend/src/main/java/com/pokemontcg/bot/**`
- `backend/src/test/java/com/pokemontcg/bot/**`

## Criterios de aceptación
- [ ] Cero imports de Spring/JPA en `com.pokemontcg.bot`; el Bot recibe exclusivamente `PlayerViewDTO`.
- [ ] `BasicBotStrategy` y `BotTurnRunner` completan partidas Bot vs. Bot en JUnit 5 sin errores ni loops infinitos (F3).
- [ ] `TacticalBotStrategy` usa evoluciones, ventaja de tipo, retirada y Entrenadores en simulaciones JUnit 5 (F4).
