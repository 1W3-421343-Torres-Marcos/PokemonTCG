import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthStore } from '../../core/auth/auth.store';

@Component({
  selector: 'app-register-page',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="auth-wrap">
      <div class="auth-card">
        <h1>Crear Cuenta</h1>
        <p class="subtitle">Regístrate como entrenador para guardar mazos propios y combatir.</p>

        @if (authStore.error(); as err) {
          <div class="error-banner" role="alert">{{ err }}</div>
        }

        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <label class="field">
            <span>Nombre de entrenador</span>
            <input type="text" formControlName="username" autocomplete="username" placeholder="AshKetchum" />
            @if (form.controls.username.touched && form.controls.username.invalid) {
              <small>Mínimo 3 caracteres (máximo 50).</small>
            }
          </label>

          <label class="field">
            <span>Correo electrónico</span>
            <input type="email" formControlName="email" autocomplete="email" placeholder="ash@pallet.town" />
            @if (form.controls.email.touched && form.controls.email.invalid) {
              <small>Ingresa un correo válido.</small>
            }
          </label>

          <label class="field">
            <span>Contraseña</span>
            <input type="password" formControlName="password" autocomplete="new-password" placeholder="Mínimo 6 caracteres" />
            @if (form.controls.password.touched && form.controls.password.invalid) {
              <small>Mínimo 6 caracteres.</small>
            }
          </label>

          <button type="submit" class="submit-btn" [disabled]="authStore.loading()">
            @if (authStore.loading()) { Creando cuenta... } @else { Registrarme }
          </button>
        </form>

        <p class="switch-link">
          ¿Ya tienes cuenta? <a routerLink="/login">Inicia sesión</a>
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
export class RegisterPageComponent implements OnInit {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly route = inject(ActivatedRoute);
  protected readonly authStore = inject(AuthStore);

  protected readonly form = this.fb.group({
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    password: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(100)]]
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
    this.authStore.register(this.form.getRawValue(), returnUrl);
  }
}
