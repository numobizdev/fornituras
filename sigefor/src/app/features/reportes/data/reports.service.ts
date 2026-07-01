import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../core/models/api-response.model';
import {
  ActiveAssignmentFilter,
  ActiveAssignmentRow,
  Page,
  ReportTotals,
} from './report.model';

/**
 * Acceso a la API de reportes (011). Los totales y las asignaciones activas llegan como JSON; la
 * exportación llega como binario (.xlsx). El servidor aplica el enmascaramiento de PII por rol.
 */
@Injectable({ providedIn: 'root' })
export class ReportsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/reports`;

  getTotals(): Observable<ReportTotals> {
    return this.http
      .get<ApiResponse<ReportTotals>>(`${this.baseUrl}/totals`)
      .pipe(map((response) => response.data));
  }

  getActiveAssignments(
    filter: ActiveAssignmentFilter,
    page: number,
    size: number,
  ): Observable<Page<ActiveAssignmentRow>> {
    return this.http
      .get<ApiResponse<Page<ActiveAssignmentRow>>>(`${this.baseUrl}/active-assignments`, {
        params: this.toParams(filter).set('page', page).set('size', size),
      })
      .pipe(map((response) => response.data));
  }

  exportActiveAssignments(filter: ActiveAssignmentFilter): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/active-assignments/export`, {
      params: this.toParams(filter),
      responseType: 'blob',
    });
  }

  private toParams(filter: ActiveAssignmentFilter): HttpParams {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filter)) {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, value as string);
      }
    }
    return params;
  }
}
