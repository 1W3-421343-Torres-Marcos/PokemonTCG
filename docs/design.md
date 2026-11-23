# Sistema de Diseño — Pokémon TCG Web

Estilo visual de mesa de juego oscura (`tabletop`) que prioriza la legibilidad del estado de combate y hace resaltar las ilustraciones de las cartas. Todos los tokens se declaran como CSS custom properties en `frontend/src/styles/_tokens.scss`.

## 1. Paleta de colores

### Superficies y texto
| Token | Valor | Uso |
| :--- | :--- | :--- |
| `--bg-tabletop` | `#0B1020` | Fondo principal del tablero y viewport |
| `--bg-surface` | `#131B2E` | Paneles laterales, tarjetas de contenedor, sidebar |
| `--bg-elevated` | `#1C2742` | Modales, hover en zonas activas, tooltips |
| `--border-subtle` | `rgba(255, 255, 255, 0.10)` | Bordes de paneles, ranuras de banca y divisores |
| `--text-primary` | `#F3F6FC` | Títulos, HP, nombres de carta y texto principal |
| `--text-secondary` | `#A6B3CC` | Subtítulos, metadata de set y texto de soporte |
| `--text-muted` | `#6E7D9B` | Contadores vacíos, etiquetas secundarias y timestamps |

### Acentos y estados
| Token | Valor | Uso |
| :--- | :--- | :--- |
| `--accent-primary` | `#FFCB05` | CTA principal, turno activo del jugador, foco/selección |
| `--accent-secondary` | `#3B82F6` | Acciones secundarias, enlaces activos, turno CPU |
| `--state-success` | `#22C55E` | Mazo válido (`60/60`), HP alto (`>50%`), victoria |
| `--state-warning` | `#F59E0B` | HP medio (`20–50%`), advertencia de estado alterado |
| `--state-danger` | `#EF4444` | HP crítico (`<20%`), daño, mazo inválido, derrota |

### Tipos de Energía
| Token | Color | Tipo | Token | Color | Tipo |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `--type-grass` | `#4CAF50` | Planta (Grass) | `--type-fighting` | `#D97706` | Lucha (Fighting) |
| `--type-fire` | `#F44336` | Fuego (Fire) | `--type-colorless` | `#CBD5E1` | Incolora (Colorless) |
| `--type-water` | `#2196F3` | Agua (Water) | `--type-darkness` | `#334155` | Oscura (Darkness) |
| `--type-lightning` | `#FACC15` | Rayo (Lightning) | `--type-metal` | `#94A3B8` | Metálica (Metal) |
| `--type-psychic` | `#A855F7` | Psíquico (Psychic) | | | |

## 2. Tipografías y escala

- **Display / títulos**: `'Outfit', 'Plus Jakarta Sans', system-ui, sans-serif`
- **Cuerpo UI**: `'Inter', system-ui, sans-serif`
- **Numérica / HP / daño / log**: `'JetBrains Mono', monospace` con `font-variant-numeric: tabular-nums`

| Token | Tamaño | Uso principal |
| :--- | :--- | :--- |
| `--text-xs` | `12px` | Badges, etiquetas de set, costos, timestamps |
| `--text-sm` | `14px` | Cuerpo secundario, descripción de cartas, log |
| `--text-base` | `16px` | Textos UI estándar, botones, formularios |
| `--text-lg` | `18px` | Subtítulos de sección, nombre de Pokémon activo |
| `--text-xl` | `22px` | Encabezados de vistas y modales |
| `--text-2xl` | `28px` | Títulos principales, banner de fin de partida |

## 3. Espaciados, radios y elevaciones

- **Espaciado base 4px**: `--space-1: 4px`, `--space-2: 8px`, `--space-3: 12px`, `--space-4: 16px`, `--space-5: 20px`, `--space-6: 24px`, `--space-8: 32px`.
- **Radios**: `--radius-sm: 6px` (badges/inputs), `--radius-card: 8px` (cartas), `--radius-md: 10px` (botones/items), `--radius-lg: 14px` (paneles/modales).
- **Sombras**:
  - `--shadow-card: 0 6px 16px rgba(0, 0, 0, 0.45)`
  - `--shadow-active-glow: 0 0 0 2px var(--accent-primary), 0 0 18px rgba(255, 203, 5, 0.30)`
  - `--shadow-modal: 0 20px 50px rgba(0, 0, 0, 0.70)`

## 4. Componentes reutilizables (`frontend/src/app/shared/ui/`)

| Componente | Propósito | Inputs / Props clave | Estados visuales |
| :--- | :--- | :--- | :--- |
| `CardTile` | Renderizar carta en catálogo, deck builder, mano o tablero | `card`, `size` (`sm`/`md`/`lg`), `faceDown`, `playable`, `selected` | Default, hover elevación, `playable` (borde resaltado), `selected` (`--shadow-active-glow`), `disabled`, reverso CSS/SVG |
| `CardDetailModal` | Inspección ampliada con `high.webp`, ataques, costos, debilidad, resistencia y texto | `card`, `isOpen`, `canAddToDeck` | Abierto con backdrop blur y acciones contextuales |
| `DiscardPileModal` | Inspeccionar las cartas de la pila de descarte propia o rival | `cards`, `ownerLabel`, `isOpen` | Grilla compacta con apertura de `CardDetailModal` al clic |
| `DeckValidationBar` | Barra en vivo del editor `/decks/:id` con reglas oficiales | `totalCount`, `maxCopiesOk`, `hasBasicPokemon`, `errors` | Inválido (`--state-danger`), completo válido (`--state-success`) |
| `EnergyBadge` | Ícono + color oficial de un tipo de energía | `type`, `size` (`xs`/`sm`/`md`), `count` | Compacto (solo ícono) o apilado con contador |
| `HpBar` | Barra de vida actual/máxima sobre Pokémon en juego | `currentHp`, `maxHp` | Verde (`>50%`), ámbar (`20–50%`), rojo (`<20%`), transición suave |
| `StatusBadge` | Indicador de condición especial en Pokémon activo | `condition` (`POISONED`/`BURNED`/`ASLEEP`/`PARALYZED`/`CONFUSED`) | Pill coloreada con ícono SVG y tooltip explicativo |
| `ActionButton` | Botón unificado de acciones de UI y combate | `variant` (`primary`/`secondary`/`danger`/`ghost`), `disabled`, `loading` | Default, hover, focus ring `--accent-primary`, disabled (`opacity: 0.45`) |
| `EventLogFeed` | Panel de Historial de Combate reproduciendo `List<GameEvent>` | `events`, `activeEventId`, `speed` (`1x`/`2x`) | Autoscroll al evento activo, distinción Jugador vs. CPU y controles `1x/2x`/Saltar |
| `MatchResultModal` | Modal de Victoria/Derrota al finalizar partida | `winner`, `reason`, `turnCount`, `prizesTaken` | Banner Victoria (`--state-success`) o Derrota (`--state-danger`) + CTAs |

## 5. Reglas de recursos visuales y animaciones

- **Proporción oficial de carta**: Todas las miniaturas y ranuras usan `aspect-ratio: 5 / 7` con `object-fit: cover` para evitar deformaciones.
- **Origen exclusivo de imágenes**: Las imágenes se cargan solo desde la URL provista por nuestro backend (`low.webp` en grillas, mano y tablero; `high.webp` en `CardDetailModal`), con `loading="lazy"` y un reverso local CSS/SVG como fallback. Prohibido hotlinking a `pokemontcg.io` o llamadas directas a TCGdex.
- **Iconografía ligera**: Los símbolos de energía y estados alterados se implementan como SVG inline propios (sin librerías de íconos pesadas).
- **Animaciones y accesibilidad**: Transiciones UI de `200–350ms`; la cola secuencial de `GameEvent` (turnos de la CPU) anima a `450–700ms` por evento con selector `1x`/`2x` y botón *Saltar*. Respetar `@media (prefers-reduced-motion: reduce)` reduciendo desplazamientos y duraciones a `0ms`.

## 6. Layouts de pantallas y zonas del tablero

- **`/login` y `/register`**: tarjeta centrada (`max-width: 420px`) sobre `--bg-tabletop` con formulario reactivo y mensajes de error en línea.
- **`/` (Home)**: hero compacto + grilla de 3 accesos rápidos (**Catálogo**, **Mis Mazos**, **Jugar vs. CPU**) + banner destacado de **Retomar partida en curso** (`/play/:matchId`) cuando existe una partida activa.
- **`/catalog`**: barra de filtros pegajosa (buscador por nombre, selector de tipo y categoría `Pokemon`/`Trainer`/`Energy`) + grilla responsiva paginada.
- **`/decks` y `/decks/:id`**: en `/decks`, grilla de mazos propios y *starter decks* clonables; en `/decks/:id`, editor dividido en **2 paneles** (`55%` catálogo filtrable a la izquierda / `45%` mazo actual a la derecha) con `DeckValidationBar` fija superior.
- **`/play/setup`**: flujo limpio en 3 pasos/tarjetas (mazo del jugador válido, mazo CPU elegible y dificultad `BASIC`/`TACTICAL`).
- **`/play/:matchId` (`100dvh` sin scroll vertical en desktop `≥1280×720`)**:
  - **Zona Rival (superior)**: mano oculta (dorsos + contador), contador de mazo, descarte inspeccionable, 6 premios boca abajo, banca (hasta 5 slots) y Pokémon Activo (PS, energías, estados).
  - **Franja central / HUD**: indicador de turno y fase, menú de ataques habilitados según energía adherida, y botones **Retirar**, **Terminar Turno** y **Rendirse**.
  - **Zona Jugador (inferior)**: Pokémon Activo, banca (hasta 5 slots), 6 premios, mazo, descarte inspeccionable y mano interactiva con resaltado de cartas jugables según la fase.
  - **Panel lateral colapsable**: `EventLogFeed` (Historial de Combate) con reproducción secuencial de los eventos del Bot.
