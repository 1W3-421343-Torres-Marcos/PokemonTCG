import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthStore } from '../../core/auth/auth.store';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="auth-wrap">
      <div class="auth-card">
        <h1>Iniciar Sesión</h1>
        <p class="subtitle">Accede para armar tus mazos y guardar tus estadísticas PvE.</p>

        @if (authStore.error(); as err) {
          <div class="error-banner" role="alert">{{ err }}</div>
        }

        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <label class="field">
            <span>Usuario o correo electrónico</span>
            <input type="text" formControlName="identifier" autocomplete="username" placeholder="ash@pallet.town" />
            @if (form.controls.identifier.touched && form.controls.identifier.invalid) {
              <small>Ingresa tu usuario o email.</small>
            }
          </label>

          <label class="field">
            <span>Contraseña</span>
            <input type="password" formControlName="password" autocomplete="current-password" placeholder="••••••" />
            @if (form.controls.password.touched && form.controls.password.invalid) {
              <small>Ingresa tu contraseña.</small>
            }
          </label>

          <button type="submit" class="submit-btn" [disabled]="authStore.loading()">
            @if (authStore.loading()) { Ingresando... } @else { Entrar }
          </button>
        </form>

        <p class="switch-link">
          ¿No tienes cuenta? <a routerLink="/register">Regístrate aquí</a>
        </p>
      </div>
    </section>
  `,
  styles: [`
    .auth-wrap {
      min-height: 72dvh;
      display: grid;
      place-items: center;
      padding: var(--space-6) var(--space-4);
    }
    .auth-card {
      width: 100%;
      max-width: 420px;
      padding: var(--space-6);
      border-radius: var(--radius-lg);
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      box-shadow: var(--shadow-card);
    }
    h1 { font-size: var(--text-xl); margin-bottom: var(--space-1); }
    .subtitle { margin: 0 0 var(--space-5); font-size: var(--text-sm); color: var(--text-secondary); }
    .error-banner {
      padding: var(--space-3);
      margin-bottom: var(--space-4);
      border-radius: var(--radius-sm);
      background: rgba(239, 68, 68, 0.16);
      border: 1px solid rgba(239, 68, 68, 0.45);
      color: #fecaca;
      font-size: var(--text-sm);
    }
    form { display: flex; flex-direction: column; gap: var(--space-4); }
    .field { display: flex; flex-direction: column; gap: 6px; font-size: var(--text-sm); color: var(--text-secondary); }
    .field input {
      padding: 10px 12px;
      border-radius: var(--radius-sm);
      border: 1px solid var(--border-subtle);
      background: var(--bg-tabletop);
      color: var(--text-primary);
      font-size: var(--text-base);
    }
    .field input:focus { outline: 2px solid var(--accent-primary); border-color: transparent; }
    .field small { color: var(--state-danger); font-size: var(--text-xs); }
    .submit-btn {
      margin-top: var(--space-2);
      padding: 11px 16px;
      border: none;
      border-radius: var(--radius-md);
      background: var(--accent-primary);
      color: #0b1020;
      font-weight: 700;
      font-size: var(--text-sm);
      cursor: pointer;
    }
    .submit-btn:disabled { opacity: 0.55; cursor: wait; }
    .switch-link { margin: var(--space-4) 0 0; text-align: center; font-size: var(--text-sm); color: var(--text-secondary); }
    .switch-link a { color: var(--accent-primary); font-weight: 600; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LoginPageComponent implements OnInit {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly route = inject(ActivatedRoute);
  protected readonly authStore = inject(AuthStore);

  protected readonly form = this.fb.group({
    identifier: ['', [Validators.required]],
    password: ['', [Validators.required]]
  });

  ngOnInit(): void {
    this.authStore.clearError();
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '/';
    this.authStore.login(this.form.getRawValue(), returnUrl);
  }
}
