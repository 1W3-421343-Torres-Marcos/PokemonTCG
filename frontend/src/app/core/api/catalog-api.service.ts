import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CardDetail, CardSearchFilters, CardSet, CardSummary, PagedResponse } from '../models/catalog.models';

@Injectable({ providedIn: 'root' })
export class CatalogApiService {
  private readonly http = inject(HttpClient);

  getSets(): Observable<CardSet[]> {
    return this.http.get<CardSet[]>('/api/sets');
  }

  searchCards(filters: CardSearchFilters): Observable<PagedResponse<CardSummary>> {
    let params = new HttpParams();
    if (filters.q?.trim()) {
      params = params.set('q', filters.q.trim());
    }
    if (filters.setId?.trim()) {
      params = params.set('setId', filters.setId.trim());
    }
    if (filters.category?.trim()) {
      params = params.set('category', filters.category.trim());
    }
    if (filters.type?.trim()) {
      params = params.set('type', filters.type.trim());
    }
    if (filters.stage?.trim()) {
      params = params.set('stage', filters.stage.trim());
    }
    if (filters.page !== undefined) {
      params = params.set('page', String(filters.page));
    }
    if (filters.size !== undefined) {
      params = params.set('size', String(filters.size));
    }
    return this.http.get<PagedResponse<CardSummary>>('/api/cards', { params });
  }

  getCardById(id: string): Observable<CardDetail> {
    return this.http.get<CardDetail>(`/api/cards/${encodeURIComponent(id)}`);
  }
}
