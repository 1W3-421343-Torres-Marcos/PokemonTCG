import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { DeckDetail, DeckSummary, SaveDeckRequest } from '../models/deck.models';

@Injectable({ providedIn: 'root' })
export class DeckApiService {
  private readonly http = inject(HttpClient);

  listMyDecks(): Observable<DeckSummary[]> {
    return this.http.get<DeckSummary[]>('/api/decks');
  }

  listStarters(): Observable<DeckSummary[]> {
    return this.http.get<DeckSummary[]>('/api/decks/starters');
  }

  getDeck(id: string): Observable<DeckDetail> {
    return this.http.get<DeckDetail>(`/api/decks/${encodeURIComponent(id)}`);
  }

  createDeck(payload: SaveDeckRequest): Observable<DeckDetail> {
    return this.http.post<DeckDetail>('/api/decks', payload);
  }

  updateDeck(id: string, payload: SaveDeckRequest): Observable<DeckDetail> {
    return this.http.put<DeckDetail>(`/api/decks/${encodeURIComponent(id)}`, payload);
  }

  deleteDeck(id: string): Observable<void> {
    return this.http.delete<void>(`/api/decks/${encodeURIComponent(id)}`);
  }

  cloneStarter(starterId: string): Observable<DeckDetail> {
    return this.http.post<DeckDetail>(`/api/decks/starters/${encodeURIComponent(starterId)}/clone`, {});
  }
}
