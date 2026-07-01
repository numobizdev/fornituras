import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import {
  CatalogItemCreateRequest,
  CatalogItemSummary,
  CatalogSummary,
  Page,
} from './catalog.model';

/**
 * Cliente del CRUD genérico de catálogos (ADR 0007). Un solo servicio sirve a todos los catálogos
 * (tipo de prenda, tallas, tipo de almacén…): el catálogo se indica por su `code`.
 */
@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/catalogs`;

  listCatalogs(): Observable<CatalogSummary[]> {
    return this.http
      .get<ApiResponse<CatalogSummary[]>>(this.baseUrl)
      .pipe(map((response) => response.data));
  }

  listItems(
    code: string,
    options: { active?: boolean; page?: number; size?: number } = {},
  ): Observable<Page<CatalogItemSummary>> {
    let params = new HttpParams();
    if (options.active !== undefined) {
      params = params.set('active', options.active);
    }
    if (options.page !== undefined) {
      params = params.set('page', options.page);
    }
    if (options.size !== undefined) {
      params = params.set('size', options.size);
    }
    return this.http
      .get<ApiResponse<Page<CatalogItemSummary>>>(`${this.baseUrl}/${code}/items`, { params })
      .pipe(map((response) => response.data));
  }

  /** Valores activos para selectores; opcionalmente acotados a un padre (jerarquía). */
  listActiveItems(code: string, parentItemId?: number | null): Observable<CatalogItemSummary[]> {
    let params = new HttpParams();
    if (parentItemId !== undefined && parentItemId !== null) {
      params = params.set('parentItemId', parentItemId);
    }
    return this.http
      .get<ApiResponse<CatalogItemSummary[]>>(`${this.baseUrl}/${code}/items/active`, { params })
      .pipe(map((response) => response.data));
  }

  getItem(itemId: number): Observable<CatalogItemSummary> {
    return this.http
      .get<ApiResponse<CatalogItemSummary>>(`${this.baseUrl}/items/${itemId}`)
      .pipe(map((response) => response.data));
  }

  createItem(code: string, request: CatalogItemCreateRequest): Observable<CatalogItemSummary> {
    return this.http
      .post<ApiResponse<CatalogItemSummary>>(`${this.baseUrl}/${code}/items`, request)
      .pipe(map((response) => response.data));
  }

  updateItem(itemId: number, request: CatalogItemCreateRequest): Observable<CatalogItemSummary> {
    return this.http
      .put<ApiResponse<CatalogItemSummary>>(`${this.baseUrl}/items/${itemId}`, request)
      .pipe(map((response) => response.data));
  }

  deactivateItem(itemId: number): Observable<void> {
    return this.http
      .patch<ApiResponse<null>>(`${this.baseUrl}/items/${itemId}/deactivate`, {})
      .pipe(map(() => undefined));
  }
}
