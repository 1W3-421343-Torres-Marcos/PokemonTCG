import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-energy-badge',
  standalone: true,
  template: `
    <span
      class="energy-badge"
      [class]="'energy-badge size-' + size()"
      [style.--badge-color]="colorVar()"
      [title]="type()">
      @switch (normalizedType()) {
        @case ('fire') {
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 2c2.8 4.1 6 6.4 6 11a6 6 0 1 1-12 0c0-2.6 1.3-4.8 3-6.6.1 1.7 1.1 2.8 2.2 3.2-.2-2.7.1-5.1.8-7.6z"/></svg>
        }
        @case ('water') {
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 2.5C8.5 7.3 6 10.7 6 14a6 6 0 1 0 12 0c0-3.3-2.5-6.7-6-11.5z"/></svg>
        }
        @case ('grass') {
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M19 4C11 4 6 8.5 5 16c3.5.5 7-.3 9.7-2.6C17.3 11.1 18.8 7.8 19 4zM6 19l5-5"/></svg>
        }
        @case ('lightning') {
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M13 2 5 14h6l-1 8 9-12h-6z"/></svg>
        }
        @case ('psychic') {
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M2 12s4-6 10-6 10 6 10 6-4 6-10 6-10-6-10-6z"/><circle cx="12" cy="12" r="2.8"/></svg>
        }
        @case ('fighting') {
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M7 10V6h2v4m2-5v5m2-4v4m2-3v3m-9 0h10v4a5 5 0 0 1-10 0v-4z"/></svg>
        }
        @case ('darkness') {
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M16.5 4.5A8.5 8.5 0 1 0 19.5 17a7 7 0 1 1-3-12.5z"/></svg>
        }
        @case ('metal') {
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3 20 8v8l-8 5-8-5V8z"/></svg>
        }
        @default {
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m12 3 2.5 5.8L20 12l-5.5 3.2L12 21l-2.5-5.8L4 12l5.5-3.2z"/></svg>
        }
      }
      @if (showLabel()) {
        <span>{{ type() }}</span>
      }
    </span>
  `,
  styles: [`
    .energy-badge {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      border-radius: 999px;
      background: color-mix(in srgb, var(--badge-color) 20%, var(--bg-elevated));
      border: 1px solid color-mix(in srgb, var(--badge-color) 65%, transparent);
      color: var(--text-primary);
      font-size: var(--text-xs);
      font-weight: 600;
      line-height: 1;
      padding: 3px 7px;
    }
    .energy-badge svg {
      fill: var(--badge-color);
      stroke: var(--badge-color);
      stroke-width: 1.4;
      flex-shrink: 0;
    }
    .size-xs { padding: 2px 5px; }
    .size-xs svg { width: 11px; height: 11px; }
    .size-sm svg { width: 14px; height: 14px; }
    .size-md { padding: 4px 10px; font-size: var(--text-sm); }
    .size-md svg { width: 16px; height: 16px; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EnergyBadgeComponent {
  readonly type = input.required<string>();
  readonly size = input<'xs' | 'sm' | 'md'>('sm');
  readonly showLabel = input<boolean>(false);

  protected readonly normalizedType = computed(() => this.type().trim().toLowerCase());
  protected readonly colorVar = computed(() => {
    switch (this.normalizedType()) {
      case 'grass': return 'var(--type-grass)';
      case 'fire': return 'var(--type-fire)';
      case 'water': return 'var(--type-water)';
      case 'lightning': return 'var(--type-lightning)';
      case 'psychic': return 'var(--type-psychic)';
      case 'fighting': return 'var(--type-fighting)';
      case 'darkness': return 'var(--type-darkness)';
      case 'metal': return 'var(--type-metal)';
      default: return 'var(--type-colorless)';
    }
  });
}
