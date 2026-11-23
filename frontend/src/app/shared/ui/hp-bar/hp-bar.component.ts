import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-hp-bar',
  standalone: true,
  template: `
    <div class="hp-wrap">
      <div class="hp-label">
        <span>PS</span>
        <span class="mono-num">{{ remainingHp() }} / {{ maxHp() }}</span>
      </div>
      <div class="hp-track" role="progressbar" [attr.aria-valuenow]="remainingHp()" [attr.aria-valuemax]="maxHp()">
        <div
          class="hp-fill"
          [style.width.%]="percent()"
          [class.high]="percent() > 50"
          [class.mid]="percent() > 20 && percent() <= 50"
          [class.low]="percent() <= 20">
        </div>
      </div>
    </div>
  `,
  styles: [`
    .hp-wrap {
      display: flex;
      flex-direction: column;
      gap: 3px;
      width: 100%;
    }
    .hp-label {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 4px;
      white-space: nowrap;
      font-size: 10.5px;
      font-weight: 700;
      color: var(--text-primary);
    }
    .hp-track {
      height: 7px;
      border-radius: 999px;
      background: rgba(255, 255, 255, 0.12);
      overflow: hidden;
    }
    .hp-fill {
      height: 100%;
      border-radius: 999px;
      transition: width 300ms ease, background-color 300ms ease;
    }
    .hp-fill.high { background: var(--state-success); }
    .hp-fill.mid { background: var(--state-warning); }
    .hp-fill.low { background: var(--state-danger); }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HpBarComponent {
  readonly currentHp = input.required<number>();
  readonly maxHp = input.required<number>();

  protected readonly remainingHp = computed(() => Math.max(0, this.currentHp()));
  protected readonly percent = computed(() => {
    const max = this.maxHp();
    if (max <= 0) {
      return 0;
    }
    return Math.max(0, Math.min(100, Math.round((this.remainingHp() / max) * 100)));
  });
}
