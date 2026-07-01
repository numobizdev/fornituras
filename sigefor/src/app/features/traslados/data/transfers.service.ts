import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../core/models/api-response.model';
import {
  Page,
  TransferCreateRequest,
  TransferDetail,
  TransferStatus,
  TransferSummary,
} from './transfer.model';

export interface TransferListParams {
  origenId?: number;
  destinoId?: number;
  status?: TransferStatus;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class TransfersService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/transfers`;

  list(params: TransferListParams = {}): Observable<Page<TransferSummary>> {
    let httpParams = new HttpParams();
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, value as string | number);
      }
    }
    return this.http
      .get<ApiResponse<Page<TransferSummary>>>(this.baseUrl, { params: httpParams })
      .pipe(map((response) => response.data));
  }

  getById(id: number): Observable<TransferDetail> {
    return this.http
      .get<ApiResponse<TransferDetail>>(`${this.baseUrl}/${id}`)
      .pipe(map((response) => response.data));
  }

  create(request: TransferCreateRequest): Observable<TransferDetail> {
    return this.http
      .post<ApiResponse<TransferDetail>>(this.baseUrl, request)
      .pipe(map((response) => response.data));
  }

  receive(id: number): Observable<TransferDetail> {
    return this.http
      .post<ApiResponse<TransferDetail>>(`${this.baseUrl}/${id}/receive`, {})
      .pipe(map((response) => response.data));
  }

  cancel(id: number): Observable<TransferDetail> {
    return this.http
      .post<ApiResponse<TransferDetail>>(`${this.baseUrl}/${id}/cancel`, {})
      .pipe(map((response) => response.data));
  }
}
