import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../core/models/api-response.model';
import {
  Page,
  WarehouseCreateRequest,
  WarehouseDetail,
  WarehouseSummary,
  WarehouseType,
} from './warehouse.model';

@Injectable({ providedIn: 'root' })
export class WarehousesService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/warehouses`;

  list(
    options: { active?: boolean; tipo?: WarehouseType; page?: number; size?: number } = {},
  ): Observable<Page<WarehouseSummary>> {
    let params = new HttpParams();
    if (options.active !== undefined) {
      params = params.set('active', options.active);
    }
    if (options.tipo !== undefined) {
      params = params.set('tipo', options.tipo);
    }
    if (options.page !== undefined) {
      params = params.set('page', options.page);
    }
    if (options.size !== undefined) {
      params = params.set('size', options.size);
    }
    return this.http
      .get<ApiResponse<Page<WarehouseSummary>>>(this.baseUrl, { params })
      .pipe(map((response) => response.data));
  }

  getById(id: number): Observable<WarehouseDetail> {
    return this.http
      .get<ApiResponse<WarehouseDetail>>(`${this.baseUrl}/${id}`)
      .pipe(map((response) => response.data));
  }

  create(request: WarehouseCreateRequest): Observable<WarehouseDetail> {
    return this.http
      .post<ApiResponse<WarehouseDetail>>(this.baseUrl, request)
      .pipe(map((response) => response.data));
  }

  update(id: number, request: WarehouseCreateRequest): Observable<WarehouseDetail> {
    return this.http
      .put<ApiResponse<WarehouseDetail>>(`${this.baseUrl}/${id}`, request)
      .pipe(map((response) => response.data));
  }

  deactivate(id: number): Observable<void> {
    return this.http
      .patch<ApiResponse<null>>(`${this.baseUrl}/${id}/deactivate`, {})
      .pipe(map(() => undefined));
  }
}
