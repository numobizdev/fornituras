import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../core/models/api-response.model';
import {
  LandingScope,
  LandingSectionAdmin,
  LandingSectionPublic,
  LandingSectionRequest,
  ReorderRequest,
} from './landing.model';

/**
 * Acceso a la API de contenido configurable de la landing (016). La cara pública no requiere sesión; el
 * home la exige; la administración es solo ADMIN. Todas las respuestas usan el envoltorio `ApiResponse`.
 */
@Injectable({ providedIn: 'root' })
export class LandingService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/landing`;

  getPublic(): Observable<LandingSectionPublic[]> {
    return this.http
      .get<ApiResponse<LandingSectionPublic[]>>(`${this.baseUrl}/public`)
      .pipe(map((response) => response.data));
  }

  getHome(): Observable<LandingSectionPublic[]> {
    return this.http
      .get<ApiResponse<LandingSectionPublic[]>>(`${this.baseUrl}/home`)
      .pipe(map((response) => response.data));
  }

  listSections(scope: LandingScope): Observable<LandingSectionAdmin[]> {
    const params = new HttpParams().set('scope', scope);
    return this.http
      .get<ApiResponse<LandingSectionAdmin[]>>(`${this.baseUrl}/sections`, { params })
      .pipe(map((response) => response.data));
  }

  createSection(request: LandingSectionRequest): Observable<LandingSectionAdmin> {
    return this.http
      .post<ApiResponse<LandingSectionAdmin>>(`${this.baseUrl}/sections`, request)
      .pipe(map((response) => response.data));
  }

  updateSection(id: number, request: LandingSectionRequest): Observable<LandingSectionAdmin> {
    return this.http
      .put<ApiResponse<LandingSectionAdmin>>(`${this.baseUrl}/sections/${id}`, request)
      .pipe(map((response) => response.data));
  }

  deactivateSection(id: number): Observable<LandingSectionAdmin> {
    return this.http
      .patch<ApiResponse<LandingSectionAdmin>>(`${this.baseUrl}/sections/${id}/deactivate`, {})
      .pipe(map((response) => response.data));
  }

  activateSection(id: number): Observable<LandingSectionAdmin> {
    return this.http
      .patch<ApiResponse<LandingSectionAdmin>>(`${this.baseUrl}/sections/${id}/activate`, {})
      .pipe(map((response) => response.data));
  }

  reorder(request: ReorderRequest): Observable<LandingSectionAdmin[]> {
    return this.http
      .patch<ApiResponse<LandingSectionAdmin[]>>(`${this.baseUrl}/sections/reorder`, request)
      .pipe(map((response) => response.data));
  }
}
