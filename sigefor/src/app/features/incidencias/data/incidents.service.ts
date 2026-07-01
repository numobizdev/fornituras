import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../core/models/api-response.model';
import {
  AlertItem,
  IncidentCreateRequest,
  IncidentStatus,
  IncidentSummary,
  IncidentUpdateRequest,
  Page,
} from './incident.model';

export interface IncidentListParams {
  estado?: IncidentStatus;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class IncidentsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/incidents`;
  private readonly alertsUrl = `${environment.apiUrl}/alerts`;

  list(params: IncidentListParams = {}): Observable<Page<IncidentSummary>> {
    let httpParams = new HttpParams();
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, value as string | number);
      }
    }
    return this.http
      .get<ApiResponse<Page<IncidentSummary>>>(this.baseUrl, { params: httpParams })
      .pipe(map((response) => response.data));
  }

  getById(id: number): Observable<IncidentSummary> {
    return this.http
      .get<ApiResponse<IncidentSummary>>(`${this.baseUrl}/${id}`)
      .pipe(map((response) => response.data));
  }

  report(request: IncidentCreateRequest): Observable<IncidentSummary> {
    return this.http
      .post<ApiResponse<IncidentSummary>>(this.baseUrl, request)
      .pipe(map((response) => response.data));
  }

  update(id: number, request: IncidentUpdateRequest): Observable<IncidentSummary> {
    return this.http
      .patch<ApiResponse<IncidentSummary>>(`${this.baseUrl}/${id}`, request)
      .pipe(map((response) => response.data));
  }

  vigenciaAlerts(): Observable<AlertItem[]> {
    return this.http
      .get<ApiResponse<AlertItem[]>>(`${this.alertsUrl}/vigencia`)
      .pipe(map((response) => response.data));
  }
}
