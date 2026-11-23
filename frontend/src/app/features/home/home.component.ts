import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

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
          <a routerLink="/play/setup" class="cta-secondary">Configurar Partida (F3)</a>
        </div>
      </section>

      <section class="resume-banner" aria-label="Partida en curso">
        <div>
          <strong>¿Partida en progreso?</strong>
          <span>Cuando empieces a combatir en F3, podrás retomar aquí tu última partida contra la CPU.</span>
        </div>
        <a routerLink="/play/setup" class="resume-link">Ir a Setup →</a>
      </section>

      <section class="quick-grid" aria-label="Accesos rápidos">
        <a routerLink="/catalog" class="quick-card">
          <span class="pill ready">Disponible</span>
          <h2>Catálogo de Cartas</h2>
          <p>Busca por nombre, filtra por tipo o categoría e inspecciona ataques y debilidades en alta resolución.</p>
        </a>

        <a routerLink="/decks" class="quick-card upcoming">
          <span class="pill soon">Fase 2</span>
          <h2>Mis Mazos</h2>
          <p>Constructor de 60 cartas con validación en vivo y mazos prearmados listos para clonar.</p>
        </a>

        <a routerLink="/play/setup" class="quick-card upcoming">
          <span class="pill soon">Fase 3</span>
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
      gap: var(--space-6);
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
export class HomeComponent {}
