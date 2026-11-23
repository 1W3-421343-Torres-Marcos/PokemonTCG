# Pokémon TCG Web (PvE)

## Resumen ejecutivo
- **Qué es**: Simulador web de Pokémon Trading Card Game en modalidad **Jugador vs. Máquina (PvE)**, integrado con un **catálogo local de cartas** sincronizado desde TCGdex API v2 y un **Deck Builder** con validación de reglas oficiales.
- **Para quién**: Jugadores casuales y aficionados a Pokémon TCG que buscan partidas rápidas contra la CPU en el navegador, y como proyecto de práctica con arquitectura modular full-stack (Java 21 + Spring Boot + Angular 18+ + PostgreSQL).
- **Propósito**: Ofrecer una experiencia de combate fiel a las reglas base de Pokémon TCG mediante un **servidor autoritativo en Java puro** (desacoplado de la capa web, testeable en milisegundos y extensible a PvP futuro) y un **cliente Angular reactivo** que reproduce eventos de juego en un tablero claro.

## Estructura general del monorepo

```text
Pokemon/
├── AGENTS.md                 # Reglas siempre activas, arquitectura y protocolo de trabajo
├── backend/                  # Proyecto Maven (Java 21 + Spring Boot 3.x + engine/bot puros)
├── frontend/                 # Aplicación Angular 18+ (Standalone Components + Signals)
├── database/                 # docker-compose.yml (PostgreSQL 16) y migrations/ (Flyway)
├── docs/
│   ├── readme.md             # Este documento: visión general y mapa del monorepo
│   ├── plan.md               # Tablero de progreso por fases y registro cronológico
│   └── design.md             # Sistema de diseño visual (paleta, tipografía, componentes)
└── .agent/
    └── skills/               # Especificaciones modulares (cargar solo la del módulo en curso)
        ├── catalog-sync/SKILL.md
        ├── auth-users/SKILL.md
        ├── deck-builder/SKILL.md
        ├── game-engine/SKILL.md
        ├── bot-ai/SKILL.md
        ├── match-api/SKILL.md
        ├── web-app-core/SKILL.md
        └── web-battle-board/SKILL.md
```

## Mapa de documentación y módulos

| Documento / Módulo | Ruta | Responsabilidad principal |
| :--- | :--- | :--- |
| Reglas globales | `AGENTS.md` | Stack, reglas innegociables, decisiones base y protocolo de ejecución |
| Tablero de avance | `docs/plan.md` | Fases incrementales (`F1`–`F4`), pasos atómicos y registro de avance |
| Sistema visual | `docs/design.md` | Tokens CSS, tipografías, componentes reutilizables y reglas del tablero |
| `catalog-sync` | `.agent/skills/catalog-sync/SKILL.md` | Integración TCGdex (`RestClient`), esquema de catálogo y endpoints REST |
| `auth-users` | `.agent/skills/auth-users/SKILL.md` | Registro, login JWT stateless, perfil, cartas favoritas y stats PvE |
| `deck-builder` | `.agent/skills/deck-builder/SKILL.md` | CRUD de mazos, validación oficial de 60 cartas y starter decks |
| `game-engine` | `.agent/skills/game-engine/SKILL.md` | Motor puro en Java, máquina de estados, daño, estados alterados y efectos |
| `bot-ai` | `.agent/skills/bot-ai/SKILL.md` | IA oponente justa (`PlayerViewDTO`), estrategias y simulación Bot vs. Bot |
| `match-api` | `.agent/skills/match-api/SKILL.md` | Persistencia `JSONB` de partidas, endpoints PvE y flujo humano + turno CPU |
| `web-app-core` | `.agent/skills/web-app-core/SKILL.md` | SPA Angular: shell, JWT guards, Auth, Home, Catálogo y Deck Builder |
| `web-battle-board` | `.agent/skills/web-battle-board/SKILL.md` | SPA Angular: Setup (`/play/setup`), Tablero (`/play/:matchId`), HUD y `EventLogFeed` |
