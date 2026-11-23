import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import {
  ActiveMatchSummary,
  CreateMatchRequest,
  MatchActionRequest,
  MatchTurnResponse
} from '../models/match.models';

@Injectable({ providedIn: 'root' })
export class MatchApiService {
  private readonly http = inject(HttpClient);

  createMatch(request: CreateMatchRequest): Observable<MatchTurnResponse> {
    return this.http.post<MatchTurnResponse>('/api/matches', request);
  }

  getActiveMatch(): Observable<ActiveMatchSummary | null> {
    return this.http
      .get<ActiveMatchSummary>('/api/matches/active', { observe: 'response' })
      .pipe(map((res) => (res.status === 204 ? null : res.body)));
  }

  getMatch(matchId: string): Observable<MatchTurnResponse> {
    return this.http.get<MatchTurnResponse>(`/api/matches/${encodeURIComponent(matchId)}`);
  }

  sendAction(matchId: string, action: MatchActionRequest): Observable<MatchTurnResponse> {
    return this.http.post<MatchTurnResponse>(`/api/matches/${encodeURIComponent(matchId)}/actions`, action);
  }

  concedeMatch(matchId: string): Observable<MatchTurnResponse> {
    return this.http.post<MatchTurnResponse>(`/api/matches/${encodeURIComponent(matchId)}/concede`, {});
  }
}
