import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

@Component({
  selector: 'app-feature-placeholder',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="placeholder-wrap">
      <div class="placeholder-card">
        <span class="phase-badge">{{ phase }}</span>
        <h1>{{ title }}</h1>
        <p>{{ description }}</p>
        <div class="actions">
          <a routerLink="/catalog" class="btn-primary">Explorar Catálogo</a>
          <a routerLink="/" class="btn-secondary">Volver al Inicio</a>
        </div>
      </div>
    </section>
  `,
  styles: [`
    .placeholder-wrap {
      display: grid;
      place-items: center;
      min-height: 60dvh;
      padding: var(--space-6);
    }
    .placeholder-card {
      max-width: 480px;
      width: 100%;
      padding: var(--space-6);
      border-radius: var(--radius-lg);
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      text-align: center;
      box-shadow: var(--shadow-card);
    }
    .phase-badge {
      display: inline-block;
      padding: 4px 10px;
      border-radius: 999px;
      font-size: var(--text-xs);
      font-weight: 700;
      background: rgba(59, 130, 246, 0.20);
      color: var(--accent-secondary);
      border: 1px solid rgba(59, 130, 246, 0.40);
      margin-bottom: var(--space-3);
    }
    h1 { font-size: var(--text-xl); margin-bottom: var(--space-2); }
    p { color: var(--text-secondary); margin: 0 0 var(--space-5); font-size: var(--text-sm); }
    .actions { display: flex; justify-content: center; gap: var(--space-3); flex-wrap: wrap; }
    .btn-primary, .btn-secondary {
      padding: 10px 16px;
      border-radius: var(--radius-md);
      font-weight: 600;
      font-size: var(--text-sm);
    }
    .btn-primary { background: var(--accent-primary); color: #0b1020; }
    .btn-secondary { background: var(--bg-elevated); color: var(--text-primary); border: 1px solid var(--border-subtle); }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FeaturePlaceholderComponent {
  private readonly data = inject(ActivatedRoute).snapshot.data;
  protected readonly title = (this.data['title'] as string) ?? 'Módulo en desarrollo';
  protected readonly description = (this.data['description'] as string) ?? 'Esta pantalla estará disponible en una próxima fase.';
  protected readonly phase = (this.data['phase'] as string) ?? 'Próximamente';
}
