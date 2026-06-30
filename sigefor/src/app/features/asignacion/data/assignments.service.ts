import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../core/models/api-response.model';
import {
  AssignRequest,
  AssignmentSummary,
  Page,
  ReassignRequest,
} from './assignment.model';

@Injectable({ providedIn: 'root' })
export class AssignmentsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/assignments`;

  listVigentes(options: { page?: number; size?: number } = {}): Observable<Page<AssignmentSummary>> {
    let params = new HttpParams();
    if (options.page !== undefined) {
      params = params.set('page', options.page);
    }
    if (options.size !== undefined) {
      params = params.set('size', options.size);
    }
    return this.http
      .get<ApiResponse<Page<AssignmentSummary>>>(this.baseUrl, { params })
      .pipe(map((response) => response.data));
  }

  assign(request: AssignRequest): Observable<AssignmentSummary> {
    return this.http
      .post<ApiResponse<AssignmentSummary>>(this.baseUrl, request)
      .pipe(map((response) => response.data));
  }

  returnAssignment(id: number): Observable<AssignmentSummary> {
    return this.http
      .post<ApiResponse<AssignmentSummary>>(`${this.baseUrl}/${id}/return`, {})
      .pipe(map((response) => response.data));
  }

  reassign(request: ReassignRequest): Observable<AssignmentSummary> {
    return this.http
      .post<ApiResponse<AssignmentSummary>>(`${this.baseUrl}/reassign`, request)
      .pipe(map((response) => response.data));
  }
}
