import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthStore } from '../../auth/auth.store';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="shell">
      <header class="topbar">
        <div class="topbar-inner">
          <a routerLink="/" class="brand">
            <span class="brand-dot" aria-hidden="true"></span>
            <span>Pokémon TCG <small>PvE</small></span>
          </a>

          <nav class="nav" aria-label="Navegación principal">
            <a routerLink="/" routerLinkActive="is-active" [routerLinkActiveOptions]="{ exact: true }">Inicio</a>
            <a routerLink="/catalog" routerLinkActive="is-active">Catálogo</a>
            <a routerLink="/decks" routerLinkActive="is-active">Mis Mazos</a>
            <a routerLink="/play/setup" routerLinkActive="is-active" class="nav-upcoming">
              Jugar vs. CPU <span class="badge">F3</span>
            </a>

            @if (authStore.isAuthenticated()) {
              <div class="user-menu">
                <span class="user-pill">{{ authStore.user()?.username ?? 'Entrenador' }}</span>
                <button type="button" class="logout-btn" (click)="authStore.logout()">Salir</button>
              </div>
            } @else {
              <a routerLink="/login" routerLinkActive="is-active">Iniciar Sesión</a>
              <a routerLink="/register" routerLinkActive="is-active" class="register-cta">Registro</a>
            }
          </nav>
        </div>
      </header>

      <main class="main-content">
        <router-outlet />
      </main>
    </div>
  `,
  styles: [`
    .shell {
      min-height: 100dvh;
      display: flex;
      flex-direction: column;
      background: var(--bg-tabletop);
    }
    .topbar {
      position: sticky;
      top: 0;
      z-index: 30;
      background: rgba(19, 27, 46, 0.92);
      backdrop-filter: blur(10px);
      border-bottom: 1px solid var(--border-subtle);
    }
    .topbar-inner {
      max-width: 1320px;
      margin: 0 auto;
      padding: var(--space-3) var(--space-5);
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: var(--space-4);
      flex-wrap: wrap;
    }
    .brand {
      display: inline-flex;
      align-items: center;
      gap: var(--space-2);
      font-family: var(--font-display);
      font-weight: 800;
      font-size: var(--text-lg);
      letter-spacing: -0.01em;
    }
    .brand small {
      color: var(--accent-primary);
      font-size: var(--text-xs);
      text-transform: uppercase;
      margin-left: 2px;
    }
    .brand-dot {
      width: 14px;
      height: 14px;
      border-radius: 50%;
      background: linear-gradient(180deg, #ef4444 45%, #f3f6fc 55%);
      border: 2px solid var(--accent-primary);
    }
    .nav {
      display: flex;
      align-items: center;
      gap: var(--space-2);
      flex-wrap: wrap;
    }
    .nav a {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 7px 12px;
      border-radius: var(--radius-sm);
      font-size: var(--text-sm);
      font-weight: 500;
      color: var(--text-secondary);
      transition: background-color 0.15s ease, color 0.15s ease;
    }
    .nav a:hover { background: var(--bg-elevated); color: var(--text-primary); }
    .nav a.is-active { background: rgba(255, 203, 5, 0.14); color: var(--accent-primary); }
    .nav-upcoming { opacity: 0.72; }
    .register-cta {
      border: 1px solid var(--border-subtle);
      background: var(--bg-elevated);
      color: var(--text-primary) !important;
    }
    .badge {
      font-size: 10px;
      font-weight: 700;
      padding: 1px 6px;
      border-radius: 999px;
      background: var(--bg-tabletop);
      border: 1px solid var(--border-subtle);
      color: var(--text-muted);
    }
    .user-menu {
      display: inline-flex;
      align-items: center;
      gap: var(--space-2);
      margin-left: var(--space-2);
      padding-left: var(--space-2);
      border-left: 1px solid var(--border-subtle);
    }
    .user-pill {
      font-size: var(--text-xs);
      font-weight: 700;
      color: var(--accent-primary);
      padding: 4px 10px;
      border-radius: 999px;
      background: rgba(255, 203, 5, 0.12);
    }
    .logout-btn {
      padding: 6px 10px;
      border-radius: var(--radius-sm);
      border: 1px solid var(--border-subtle);
      background: var(--bg-tabletop);
      color: var(--text-secondary);
      font-size: var(--text-xs);
      cursor: pointer;
    }
    .logout-btn:hover { color: var(--text-primary); border-color: var(--state-danger); }
    .main-content { flex: 1; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppShellComponent {
  protected readonly authStore = inject(AuthStore);
}
