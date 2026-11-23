import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

interface StatusMeta {
  label: string;
  cssClass: string;
}

const STATUS_MAP: Record<string, StatusMeta> = {
  POISONED: { label: 'Veneno', cssClass: 'poisoned' },
  BURNED: { label: 'Quemado', cssClass: 'burned' },
  ASLEEP: { label: 'Dormido', cssClass: 'asleep' },
  PARALYZED: { label: 'Paralizado', cssClass: 'paralyzed' },
  CONFUSED: { label: 'Confundido', cssClass: 'confused' }
};

@Component({
  selector: 'app-status-badge',
  standalone: true,
  template: `
    <span class="status-pill" [class]="meta().cssClass" [title]="meta().label">
      {{ meta().label }}
    </span>
  `,
  styles: [`
    .status-pill {
      display: inline-flex;
      align-items: center;
      padding: 2px 7px;
      border-radius: 999px;
      font-size: 10px;
      font-weight: 800;
      letter-spacing: 0.03em;
      text-transform: uppercase;
      line-height: 1.2;
      border: 1px solid transparent;
    }
    .status-pill.poisoned {
      background: rgba(168, 85, 247, 0.22);
      color: #e9d5ff;
      border-color: rgba(168, 85, 247, 0.6);
    }
    .status-pill.burned {
      background: rgba(249, 115, 22, 0.22);
      color: #fed7aa;
      border-color: rgba(249, 115, 22, 0.6);
    }
    .status-pill.asleep {
      background: rgba(56, 189, 248, 0.22);
      color: #bae6fd;
      border-color: rgba(56, 189, 248, 0.6);
    }
    .status-pill.paralyzed {
      background: rgba(234, 179, 8, 0.24);
      color: #fef08a;
      border-color: rgba(234, 179, 8, 0.65);
    }
    .status-pill.confused {
      background: rgba(236, 72, 153, 0.22);
      color: #fbcfe8;
      border-color: rgba(236, 72, 153, 0.6);
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StatusBadgeComponent {
  readonly condition = input.required<string>();

  protected readonly meta = computed<StatusMeta>(() => {
    const key = this.condition().trim().toUpperCase();
    return STATUS_MAP[key] ?? { label: key, cssClass: 'confused' };
  });
}
