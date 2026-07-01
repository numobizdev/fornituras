import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../core/models/api-response.model';
import {
  DecommissionReasonItem,
  DecommissionRequest,
  DecommissionSummary,
  Page,
} from './decommission.model';

export interface DecommissionListParams {
  fechaDesde?: string;
  fechaHasta?: string;
  tipoId?: number;
  motivoId?: number;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class DecommissionsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/decommissions`;

  list(params: DecommissionListParams = {}): Observable<Page<DecommissionSummary>> {
    let httpParams = new HttpParams();
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, value as string | number);
      }
    }
    return this.http
      .get<ApiResponse<Page<DecommissionSummary>>>(this.baseUrl, { params: httpParams })
      .pipe(map((response) => response.data));
  }

  reasons(): Observable<DecommissionReasonItem[]> {
    return this.http
      .get<ApiResponse<DecommissionReasonItem[]>>(`${this.baseUrl}/reasons`)
      .pipe(map((response) => response.data));
  }

  decommission(request: DecommissionRequest): Observable<DecommissionSummary> {
    return this.http
      .post<ApiResponse<DecommissionSummary>>(this.baseUrl, request)
      .pipe(map((response) => response.data));
  }
}
