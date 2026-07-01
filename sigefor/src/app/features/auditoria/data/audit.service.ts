import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../core/models/api-response.model';
import { AuditFilter, AuditLogSummary, Page } from './audit.model';

/** Acceso a la bitácora de auditoría (012). Solo lectura; el backend la restringe a ADMIN. */
@Injectable({ providedIn: 'root' })
export class AuditService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/audit`;

  query(filter: AuditFilter, page: number, size: number): Observable<Page<AuditLogSummary>> {
    let params = new HttpParams().set('page', page).set('size', size);
    for (const [key, value] of Object.entries(filter)) {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, value as string);
      }
    }
    return this.http
      .get<ApiResponse<Page<AuditLogSummary>>>(this.baseUrl, { params })
      .pipe(map((response) => response.data));
  }
}
