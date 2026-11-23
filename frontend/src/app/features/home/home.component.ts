import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, of, switchMap } from 'rxjs';
import { MatchApiService } from '../../core/api/match-api.service';
import { AuthStore } from '../../core/auth/auth.store';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="home-container">
      <section class="hero">
        <span class="hero-tag">Simulador PvE · Base Set Local</span>
        <h1>Pokémon TCG Web</h1>
        <p>
          Explora el catálogo sincronizado desde PostgreSQL, arma mazos oficiales de 60 cartas
          y desafía a la CPU en partidas tácticas por turnos.
        </p>
        <div class="hero-actions">
          <a routerLink="/catalog" class="cta-primary">Explorar Catálogo</a>
          <a routerLink="/decks" class="cta-secondary">Mis Mazos</a>
        </div>
      </section>

      @if (authStore.user(); as profile) {
        <section class="profile-summary" aria-label="Perfil de entrenador">
          <div>
            <span class="summary-label">Entrenador conectado</span>
            <h2>{{ profile.username }}</h2>
          </div>
          <div class="summary-stats">
            <div class="stat-chip">
              <span class="mono-num">{{ profile.matchesPlayed }}</span>
              <small>Partidas</small>
            </div>
            <div class="stat-chip">
              <span class="mono-num">{{ profile.matchesWon }}</span>
              <small>Victorias</small>
            </div>
            <div class="stat-chip">
              <span class="mono-num">{{ profile.favoriteCardIds.length }}</span>
              <small>Favoritas</small>
            </div>
          </div>
        </section>
      }

      @if (activeMatch(); as match) {
        <section class="resume-banner active-match" aria-label="Partida en curso">
          <div>
            <strong>Partida en curso · Turno {{ match.turnNumber }}</strong>
            <span>{{ match.playerDeckName }} vs. {{ match.cpuDeckName }} ({{ match.botDifficulty }})</span>
          </div>
          <a [routerLink]="['/play', match.matchId]" class="cta-primary">Retomar partida en curso →</a>
        </section>
      } @else {
        <section class="resume-banner" aria-label="Partida rápida">
          <div>
            <strong>¿Listo para combatir contra la CPU?</strong>
            <span>Elige tu mazo de 60 cartas, selecciona el rival y juega por turnos en el tablero PvE.</span>
          </div>
          <a routerLink="/play/setup" class="resume-link">Configurar Partida →</a>
        </section>
      }

      <section class="quick-grid" aria-label="Accesos rápidos">
        <a routerLink="/catalog" class="quick-card">
          <span class="pill ready">Disponible</span>
          <h2>Catálogo de Cartas</h2>
          <p>Busca por nombre, filtra por tipo o categoría e inspecciona ataques y debilidades en alta resolución.</p>
        </a>

        <a routerLink="/decks" class="quick-card">
          <span class="pill ready">Disponible</span>
          <h2>Mis Mazos</h2>
          <p>Constructor de 60 cartas con validación en vivo y mazos prearmados listos para clonar.</p>
        </a>

        <a routerLink="/play/setup" class="quick-card">
          <span class="pill ready">Disponible</span>
          <h2>Jugar vs. CPU</h2>
          <p>Selecciona tu mazo, elige la dificultad del Bot y combate en un tablero interactivo con historial.</p>
        </a>
      </section>
    </div>
  `,
  styles: [`
    .home-container {
      max-width: 1120px;
      margin: 0 auto;
      padding: var(--space-8) var(--space-5);
      display: flex;
      flex-direction: column;
      gap: var(--space-5);
    }
    .hero {
      padding: var(--space-8);
      border-radius: var(--radius-lg);
      background: linear-gradient(135deg, #131b2e 0%, #1c2742 100%);
      border: 1px solid var(--border-subtle);
      box-shadow: var(--shadow-card);
    }
    .hero-tag {
      display: inline-block;
      font-size: var(--text-xs);
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.06em;
      color: var(--accent-primary);
      margin-bottom: var(--space-2);
    }
    .hero h1 { font-size: clamp(28px, 4vw, 40px); margin-bottom: var(--space-3); }
    .hero p { max-width: 640px; color: var(--text-secondary); margin: 0 0 var(--space-5); }
    .hero-actions { display: flex; gap: var(--space-3); flex-wrap: wrap; }
    .cta-primary, .cta-secondary {
      padding: 11px 18px;
      border-radius: var(--radius-md);
      font-weight: 600;
      font-size: var(--text-sm);
    }
    .cta-primary { background: var(--accent-primary); color: #0b1020; }
    .cta-secondary { background: var(--bg-tabletop); border: 1px solid var(--border-subtle); color: var(--text-primary); }
    .profile-summary {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: var(--space-4);
      flex-wrap: wrap;
      padding: var(--space-4) var(--space-5);
      border-radius: var(--radius-lg);
      background: var(--bg-surface);
      border: 1px solid rgba(255, 203, 5, 0.25);
    }
    .summary-label { font-size: var(--text-xs); color: var(--accent-primary); text-transform: uppercase; font-weight: 700; }
    .profile-summary h2 { font-size: var(--text-xl); margin-top: 2px; }
    .summary-stats { display: flex; gap: var(--space-4); }
    .stat-chip {
      text-align: center;
      padding: var(--space-2) var(--space-4);
      border-radius: var(--radius-md);
      background: var(--bg-tabletop);
      border: 1px solid var(--border-subtle);
    }
    .stat-chip span { display: block; font-size: var(--text-lg); font-weight: 700; color: var(--accent-primary); }
    .stat-chip small { font-size: var(--text-xs); color: var(--text-muted); }
    .resume-banner {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: var(--space-4);
      flex-wrap: wrap;
      padding: var(--space-4) var(--space-5);
      border-radius: var(--radius-md);
      background: var(--bg-surface);
      border: 1px dashed var(--border-subtle);
    }
    .resume-banner strong { display: block; font-size: var(--text-sm); }
    .resume-banner span { font-size: var(--text-xs); color: var(--text-secondary); }
    .resume-banner.active-match {
      border-style: solid;
      border-color: rgba(255, 203, 5, 0.45);
      background: linear-gradient(135deg, #131b2e 0%, #1c2742 100%);
    }
    .resume-link { font-size: var(--text-sm); font-weight: 600; color: var(--accent-secondary); }
    .quick-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
      gap: var(--space-4);
    }
    .quick-card {
      padding: var(--space-5);
      border-radius: var(--radius-lg);
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      transition: transform 0.2s ease, border-color 0.2s ease;
    }
    .quick-card:hover { transform: translateY(-3px); border-color: rgba(255, 203, 5, 0.45); }
    .quick-card.upcoming { opacity: 0.85; }
    .quick-card h2 { font-size: var(--text-lg); margin: var(--space-3) 0 var(--space-2); }
    .quick-card p { margin: 0; font-size: var(--text-sm); color: var(--text-secondary); }
    .pill {
      font-size: var(--text-xs);
      font-weight: 700;
      padding: 3px 9px;
      border-radius: 999px;
    }
    .pill.ready { background: rgba(34, 197, 94, 0.16); color: var(--state-success); }
    .pill.soon { background: rgba(59, 130, 246, 0.18); color: var(--accent-secondary); }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HomeComponent {
  protected readonly authStore = inject(AuthStore);
  private readonly matchApi = inject(MatchApiService);

  protected readonly activeMatch = toSignal(
    toObservable(this.authStore.isAuthenticated).pipe(
      switchMap((authenticated) =>
        authenticated ? this.matchApi.getActiveMatch().pipe(catchError(() => of(null))) : of(null)
      )
    ),
    { initialValue: null }
  );
}
