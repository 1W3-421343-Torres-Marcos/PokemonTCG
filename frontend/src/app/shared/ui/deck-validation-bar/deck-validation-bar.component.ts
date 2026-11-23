import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-deck-validation-bar',
  standalone: true,
  template: `
    <div class="validation-bar" [class.is-valid]="isValid()" [class.is-invalid]="!isValid()">
      <div class="checks-row">
        <span class="count-pill mono-num" [class.ok]="totalCards() === 60">
          {{ totalCards() }} / 60 cartas
        </span>

        <span class="rule-chip" [class.ok]="hasBasicPokemon()">
          @if (hasBasicPokemon()) { ✓ } @else { ✕ } ≥1 Pokémon Básico
        </span>

        <span class="rule-chip" [class.ok]="copiesLimitOk()">
          @if (copiesLimitOk()) { ✓ } @else { ✕ } Máx. 4 copias por nombre (salvo Energías Básicas)
        </span>

        <span class="status-label">
          @if (isValid()) { Mazo válido para combatir } @else { Borrador incompleto }
        </span>
      </div>

      @if (errors().length > 0) {
        <ul class="error-list" aria-label="Errores de validación del mazo">
          @for (err of errors(); track err) {
            <li>{{ err }}</li>
          }
        </ul>
      }
    </div>
  `,
  styles: [`
    .validation-bar {
      padding: var(--space-3) var(--space-4);
      border-radius: var(--radius-md);
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      display: flex;
      flex-direction: column;
      gap: var(--space-2);
    }
    .validation-bar.is-valid {
      border-color: rgba(34, 197, 94, 0.55);
      background: rgba(34, 197, 94, 0.08);
    }
    .validation-bar.is-invalid {
      border-color: rgba(245, 158, 11, 0.45);
    }
    .checks-row {
      display: flex;
      align-items: center;
      gap: var(--space-2);
      flex-wrap: wrap;
    }
    .count-pill, .rule-chip {
      font-size: var(--text-xs);
      font-weight: 600;
      padding: 4px 10px;
      border-radius: 999px;
      background: var(--bg-tabletop);
      border: 1px solid var(--border-subtle);
      color: var(--text-secondary);
    }
    .count-pill.ok, .rule-chip.ok {
      color: var(--state-success);
      border-color: rgba(34, 197, 94, 0.45);
      background: rgba(34, 197, 94, 0.14);
    }
    .status-label {
      margin-left: auto;
      font-size: var(--text-xs);
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.04em;
      color: var(--text-secondary);
    }
    .is-valid .status-label { color: var(--state-success); }
    .error-list {
      margin: 0;
      padding-left: var(--space-5);
      font-size: var(--text-xs);
      color: #fca5a5;
      display: flex;
      flex-wrap: wrap;
      gap: var(--space-1) var(--space-5);
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DeckValidationBarComponent {
  readonly totalCards = input.required<number>();
  readonly hasBasicPokemon = input.required<boolean>();
  readonly copiesLimitOk = input.required<boolean>();
  readonly isValid = input.required<boolean>();
  readonly errors = input<string[]>([]);
}
