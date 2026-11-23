import { Injectable, signal } from '@angular/core';
import { GameEvent } from '../../core/models/match.models';

@Injectable({ providedIn: 'root' })
export class EventAnimatorService {
  readonly isAnimating = signal<boolean>(false);
  readonly playbackSpeed = signal<1 | 2>(1);
  readonly activeEvent = signal<GameEvent | null>(null);

  private timerId: ReturnType<typeof setTimeout> | null = null;
  private queue: GameEvent[] = [];
  private cursor = 0;
  private onStepCb: ((event: GameEvent) => void) | null = null;
  private onDoneCb: (() => void) | null = null;

  /**
   * Reproduces GameEvent entries sequentially inside the log/indicator while
   * applying the server's final PlayerViewDTO without simulating intermediate board states.
   */
  play(
    events: GameEvent[],
    onStep: (event: GameEvent) => void,
    onDone: () => void
  ): void {
    this.cancel();

    if (events.length === 0) {
      this.isAnimating.set(false);
      this.activeEvent.set(null);
      onDone();
      return;
    }

    this.queue = [...events];
    this.cursor = 0;
    this.onStepCb = onStep;
    this.onDoneCb = onDone;
    this.isAnimating.set(true);

    this.advance();
  }

  setSpeed(speed: 1 | 2): void {
    this.playbackSpeed.set(speed);
  }

  skip(): void {
    if (!this.isAnimating()) {
      return;
    }
    if (this.timerId !== null) {
      clearTimeout(this.timerId);
      this.timerId = null;
    }

    const step = this.onStepCb;
    const done = this.onDoneCb;
    while (this.cursor < this.queue.length) {
      const evt = this.queue[this.cursor++];
      step?.(evt);
    }
    this.finish(done);
  }

  cancel(): void {
    if (this.timerId !== null) {
      clearTimeout(this.timerId);
      this.timerId = null;
    }
    this.queue = [];
    this.cursor = 0;
    this.onStepCb = null;
    this.onDoneCb = null;
    this.activeEvent.set(null);
    this.isAnimating.set(false);
  }

  private advance(): void {
    if (this.cursor >= this.queue.length) {
      const settleDelay = this.playbackSpeed() === 2 ? 120 : 240;
      const done = this.onDoneCb;
      this.timerId = setTimeout(() => this.finish(done), settleDelay);
      return;
    }

    const evt = this.queue[this.cursor++];
    this.activeEvent.set(evt);
    this.onStepCb?.(evt);

    const stepDelay = this.playbackSpeed() === 2 ? 225 : 460;
    this.timerId = setTimeout(() => this.advance(), stepDelay);
  }

  private finish(done: (() => void) | null): void {
    this.queue = [];
    this.cursor = 0;
    this.onStepCb = null;
    this.onDoneCb = null;
    this.activeEvent.set(null);
    this.isAnimating.set(false);
    done?.();
  }
}
