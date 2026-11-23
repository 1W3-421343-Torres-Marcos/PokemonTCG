import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest, UserProfile } from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly http = inject(HttpClient);

  register(payload: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/register', payload);
  }

  login(payload: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/login', payload);
  }

  getMe(): Observable<UserProfile> {
    return this.http.get<UserProfile>('/api/users/me');
  }

  addFavorite(cardId: string): Observable<UserProfile> {
    return this.http.put<UserProfile>(`/api/users/me/favorites/${encodeURIComponent(cardId)}`, {});
  }

  removeFavorite(cardId: string): Observable<UserProfile> {
    return this.http.delete<UserProfile>(`/api/users/me/favorites/${encodeURIComponent(cardId)}`);
  }
}
