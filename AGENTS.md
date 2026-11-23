# Pokémon TCG Web (PvE) — Reglas del Proyecto

## Stack tecnológico
- **Backend (`/backend`)**: Java 21, Spring Boot 3.x (Maven), Spring Web (REST), Spring Data JPA, Spring Security + JWT, PostgreSQL 16, Flyway, JUnit 5 + Mockito.
- **Frontend (`/frontend`)**: Angular 18+ (Standalone Components, Signals, TypeScript, SCSS con CSS custom properties).
- **Base de datos (`/database`)**: Docker Compose + PostgreSQL 16 + migraciones Flyway (`/database/migrations`).
- **API externa**: TCGdex API v2 (`https://api.tcgdex.net/v2/`), gratuita y sin API key. Prohibido usar `pokemontcg.io`.

## Estructura del monorepo
- `/backend`: API REST, persistencia y paquetes puros `engine` / `bot`.
- `/frontend`: cliente web Angular (`docs/design.md`).
- `/database`: `docker-compose.yml` y `migrations/` (Flyway).
- `/docs`: `readme.md` (resumen), `plan.md` (tablero de progreso) y `design.md` (sistema visual).
- `/.agent/skills/<modulo>/SKILL.md`: especificación exclusiva de cada módulo; leer solo la del módulo en curso.

## Arquitectura y reglas innegociables
1. **Caché/sincronización local**: El backend sincroniza sets acotados de TCGdex a PostgreSQL; el frontend nunca consulta TCGdex (evita latencia, rate-limits y acoplamiento).
2. **Servidor autoritativo REST + Event Log**: PvE sin WebSockets ni matchmaking; cada `POST /api/matches/{id}/actions` corre en Java y devuelve `PlayerViewDTO` + `List<GameEvent>` cronológica (humano + CPU) para animar e historizar.
3. **Angular como cliente tonto**: Solo renderiza `PlayerViewDTO`, reproduce `GameEvent` y envía `GameAction`, sin reglas ni cálculo de daño.
4. **Separación Datos vs. Lógica de cartas**: Patrón Strategy/Registry (`Map<String, CardEffect>` por ID de TCGdex) con bloques reutilizables, sin condicionales gigantes.
5. **Máquina de estados del turno (Patrón State)**: `SETUP -> DRAW_PHASE -> MAIN_PHASE -> ATTACK_PHASE -> POKEMON_CHECKUP -> GAME_OVER`; toda acción humana o Bot se valida contra el estado y turno actual.
6. **Información oculta y Bot justo**: Nunca exponer `GameState` crudo; proyectar `PlayerViewDTO` (mano/mazo rival solo contadores). El Bot decide solo con su `PlayerViewDTO` y emite `GameAction` estándar.
7. **Desacoplamiento del núcleo**: `com.pokemontcg.engine` y `com.pokemontcg.bot` en Java puro (sin Spring/JPA) para tests en milisegundos con JUnit 5 y PvP futuro sin reescribir reglas.

## Decisiones y supuestos base
- **Monolito Maven (`/backend`)**: proyecto único con paquetes aislados (`engine` y `bot` Java puro) para build simple y tests instantáneos.
- **Flyway**: scripts en `/database/migrations` (mapeados a `db/migration`) sobre PostgreSQL 16 vía `/database/docker-compose.yml`.
- **Set inicial TCGdex**: `base1` (102 cartas) en `en` para garantizar metadatos de combate; idioma configurable a futuro.
- **Persistencia de partidas**: snapshot `JSONB` de `GameState` en PostgreSQL con `@Version` (bloqueo optimista), evitando Redis.
- **Colección abierta (MVP)**: todas las cartas sincronizadas disponibles en Deck Builder (perfil guarda favoritas y stats PvE).
- **Estilos Angular**: SCSS + CSS custom properties (`docs/design.md`) sin librerías UI pesadas para control total del tablero.

## Convenciones de código
- **Java**: `record` para DTOs/acciones/eventos; excepciones mapeadas en `@RestControllerAdvice`; tests JUnit 5 + Mockito (sin `@SpringBootTest` en `engine`/`bot`); `RandomProvider` inyectable.
- **Angular**: Standalone Components, `OnPush`, Signals, `@if`/`@for`/`@switch`, TypeScript estricto (`strict: true`).
- **SQL/Flyway**: `V{N}__{descripcion_snake_case}.sql`, inmutables tras aplicarse.
- **Git**: rama `main`, Conventional Commits (`feat(F1.1):`, `fix:`, `docs:`, `test:`, `chore:`), commits atómicos.

## Lo que el agente NUNCA debe hacer
- Nunca llamar a `pokemontcg.io` ni consumir TCGdex desde `/frontend`.
- Nunca introducir WebSockets ni matchmaking en el alcance inicial.
- Nunca importar Spring, JPA o Jackson-web dentro de `com.pokemontcg.engine` o `com.pokemontcg.bot`.
- Nunca exponer `GameState` crudo ni mano/mazo del oponente en respuestas REST ni dárselos al Bot.
- Nunca calcular daño, legalidad de reglas ni lógica de IA en Angular.
- Nunca usar `if/else` gigantes por carta en lugar de `CardEffectRegistry`.
- Nunca iniciar un paso nuevo sin actualizar `docs/plan.md` ni escribir código antes de aprobar el Implementation Plan al planificar.
- Nunca commitear secretos (`.env`, claves JWT) ni builds (`target/`, `node_modules/`, `dist/`), ni hacer force-push a `main`.

## Protocolo de trabajo

1. Cuando el usuario use `/plan` o pida planificar una fase o paso (ej: "/plan F1"), el agente DEBE:
   a. Leer esa fase en `docs/plan.md` y las skills de los módulos involucrados (solo esas).
   b. Generar el artefacto nativo de Antigravity: Implementation Plan + Task List, con diagramas cuando aporten (arquitectura, flujos, modelo de datos). No responder con un plan en texto plano en el chat.
   c. Detenerse y esperar la aprobación del usuario. No escribir código antes.
2. Al ejecutar, trabajar paso por paso siguiendo la Task List aprobada.
3. REGLA ESTRICTA DE REGISTRO DE AVANCE: Al terminar CADA paso atómico (ej. F1.1), y como bloqueo obligatorio antes de iniciar el siguiente (ej. F1.2), actualizar `docs/plan.md`: marcar el checkbox del paso, recalcular estado y % en la tabla de resumen, y agregar una fila nueva en "Registro de avance" (fecha, paso completado, qué quedó verificado/probado y qué paso sigue). Nunca acumular varios pasos sin registrar.
4. Al terminar una fase, marcar los criterios de aceptación en los SKILL.md de los módulos involucrados y resumir qué quedó usable.
5. Leer solo la skill del módulo que se está tocando. No cargar documentación de otros módulos.
6. Control de versiones (Git): al cerrar cada paso atómico (junto con `docs/plan.md`), hacer commit con Conventional Commits; pushear a `origin` al cerrar cada fase o cuando el usuario lo pida.
