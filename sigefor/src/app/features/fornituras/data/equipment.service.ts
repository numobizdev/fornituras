import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../core/models/api-response.model';
import {
  BatchCreateRequest,
  EquipmentCreateRequest,
  EquipmentDetail,
  EquipmentStatus,
  EquipmentSummary,
  Page,
} from './equipment.model';

export interface EquipmentListParams {
  q?: string;
  status?: EquipmentStatus;
  equipmentTypeId?: number;
  sizeId?: number;
  warehouseId?: number;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class EquipmentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/equipment`;

  list(params: EquipmentListParams = {}): Observable<Page<EquipmentSummary>> {
    let httpParams = new HttpParams();
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, value as string | number);
      }
    }
    return this.http
      .get<ApiResponse<Page<EquipmentSummary>>>(this.baseUrl, { params: httpParams })
      .pipe(map((response) => response.data));
  }

  getById(id: number): Observable<EquipmentDetail> {
    return this.http
      .get<ApiResponse<EquipmentDetail>>(`${this.baseUrl}/${id}`)
      .pipe(map((response) => response.data));
  }

  getByCodigo(codigo: string): Observable<EquipmentDetail> {
    return this.http
      .get<ApiResponse<EquipmentDetail>>(`${this.baseUrl}/by-codigo/${encodeURIComponent(codigo)}`)
      .pipe(map((response) => response.data));
  }

  create(request: EquipmentCreateRequest): Observable<EquipmentDetail> {
    return this.http
      .post<ApiResponse<EquipmentDetail>>(this.baseUrl, request)
      .pipe(map((response) => response.data));
  }

  createBatch(request: BatchCreateRequest): Observable<EquipmentDetail[]> {
    return this.http
      .post<ApiResponse<EquipmentDetail[]>>(`${this.baseUrl}/batch`, request)
      .pipe(map((response) => response.data));
  }

  update(id: number, request: EquipmentCreateRequest): Observable<EquipmentDetail> {
    return this.http
      .put<ApiResponse<EquipmentDetail>>(`${this.baseUrl}/${id}`, request)
      .pipe(map((response) => response.data));
  }

  changeStatus(id: number, status: EquipmentStatus): Observable<EquipmentDetail> {
    return this.http
      .patch<ApiResponse<EquipmentDetail>>(`${this.baseUrl}/${id}/status`, { status })
      .pipe(map((response) => response.data));
  }
}
