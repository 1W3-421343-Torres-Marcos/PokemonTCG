import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, finalize, of, tap } from 'rxjs';
import { AuthApiService } from '../api/auth-api.service';
import { LoginRequest, RegisterRequest, UserProfile } from '../models/auth.models';

const TOKEN_STORAGE_KEY = 'pokemontcg_token';

@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly api = inject(AuthApiService);
  private readonly router = inject(Router);

  readonly token = signal<string | null>(readStoredToken());
  readonly user = signal<UserProfile | null>(null);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly isAuthenticated = computed(() => Boolean(this.token()));
  readonly favoriteCardIds = computed(() => new Set(this.user()?.favoriteCardIds ?? []));

  hydrateProfile(): void {
    const currentToken = this.token();
    if (!currentToken) {
      return;
    }
    this.api
      .getMe()
      .pipe(
        catchError(() => {
          this.clearSession();
          return of(null);
        })
      )
      .subscribe((profile) => {
        if (profile) {
          this.user.set(profile);
        }
      });
  }

  login(request: LoginRequest, returnUrl = '/'): void {
    this.loading.set(true);
    this.error.set(null);
    this.api
      .login(request)
      .pipe(
        tap((res) => {
          this.persistSession(res.token, res.user);
          void this.router.navigateByUrl(returnUrl || '/');
        }),
        catchError((err: HttpErrorResponse) => {
          this.error.set(extractErrorMessage(err, 'Credenciales inválidas.'));
          return of(null);
        }),
        finalize(() => this.loading.set(false))
      )
      .subscribe();
  }

  register(request: RegisterRequest, returnUrl = '/'): void {
    this.loading.set(true);
    this.error.set(null);
    this.api
      .register(request)
      .pipe(
        tap((res) => {
          this.persistSession(res.token, res.user);
          void this.router.navigateByUrl(returnUrl || '/');
        }),
        catchError((err: HttpErrorResponse) => {
          this.error.set(extractErrorMessage(err, 'No se pudo completar el registro.'));
          return of(null);
        }),
        finalize(() => this.loading.set(false))
      )
      .subscribe();
  }

  toggleFavorite(cardId: string): void {
    if (!this.isAuthenticated()) {
      return;
    }
    const isFav = this.favoriteCardIds().has(cardId);
    const request$ = isFav ? this.api.removeFavorite(cardId) : this.api.addFavorite(cardId);
    request$
      .pipe(catchError(() => of(null)))
      .subscribe((updated) => {
        if (updated) {
          this.user.set(updated);
        }
      });
  }

  logout(): void {
    this.clearSession();
    void this.router.navigate(['/']);
  }

  clearSession(): void {
    this.token.set(null);
    this.user.set(null);
    writeStoredToken(null);
  }

  clearError(): void {
    this.error.set(null);
  }

  private persistSession(token: string, user: UserProfile): void {
    this.token.set(token);
    this.user.set(user);
    writeStoredToken(token);
  }
}

function readStoredToken(): string | null {
  try {
    return typeof localStorage !== 'undefined' ? localStorage.getItem(TOKEN_STORAGE_KEY) : null;
  } catch {
    return null;
  }
}

function writeStoredToken(token: string | null): void {
  try {
    if (typeof localStorage === 'undefined') {
      return;
    }
    if (token) {
      localStorage.setItem(TOKEN_STORAGE_KEY, token);
    } else {
      localStorage.removeItem(TOKEN_STORAGE_KEY);
    }
  } catch {
    // ignore storage errors
  }
}

function extractErrorMessage(err: HttpErrorResponse, fallback: string): string {
  if (err.error && typeof err.error === 'object' && 'message' in err.error && typeof err.error.message === 'string') {
    return err.error.message;
  }
  return fallback;
}
