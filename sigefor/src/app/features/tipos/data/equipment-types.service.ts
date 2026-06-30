import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../core/models/api-response.model';
import {
  EquipmentTypeCreateRequest,
  EquipmentTypeDetail,
  EquipmentTypeSummary,
  Page,
  SizeCreateRequest,
  SizeSummary,
} from './equipment-type.model';

@Injectable({ providedIn: 'root' })
export class EquipmentTypesService {
  private readonly http = inject(HttpClient);
  private readonly typesUrl = `${environment.apiUrl}/equipment-types`;
  private readonly sizesUrl = `${environment.apiUrl}/sizes`;

  list(options: { active?: boolean; page?: number; size?: number } = {}): Observable<Page<EquipmentTypeSummary>> {
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
      .get<ApiResponse<Page<EquipmentTypeSummary>>>(this.typesUrl, { params })
      .pipe(map((response) => response.data));
  }

  getById(id: number): Observable<EquipmentTypeDetail> {
    return this.http
      .get<ApiResponse<EquipmentTypeDetail>>(`${this.typesUrl}/${id}`)
      .pipe(map((response) => response.data));
  }

  create(request: EquipmentTypeCreateRequest): Observable<EquipmentTypeDetail> {
    return this.http
      .post<ApiResponse<EquipmentTypeDetail>>(this.typesUrl, request)
      .pipe(map((response) => response.data));
  }

  update(id: number, request: EquipmentTypeCreateRequest): Observable<EquipmentTypeDetail> {
    return this.http
      .put<ApiResponse<EquipmentTypeDetail>>(`${this.typesUrl}/${id}`, request)
      .pipe(map((response) => response.data));
  }

  deactivate(id: number): Observable<void> {
    return this.http
      .patch<ApiResponse<null>>(`${this.typesUrl}/${id}/deactivate`, {})
      .pipe(map(() => undefined));
  }

  listSizes(equipmentTypeId?: number): Observable<SizeSummary[]> {
    let params = new HttpParams();
    if (equipmentTypeId !== undefined) {
      params = params.set('equipmentTypeId', equipmentTypeId);
    }
    return this.http
      .get<ApiResponse<SizeSummary[]>>(this.sizesUrl, { params })
      .pipe(map((response) => response.data));
  }

  createSize(request: SizeCreateRequest): Observable<SizeSummary> {
    return this.http
      .post<ApiResponse<SizeSummary>>(this.sizesUrl, request)
      .pipe(map((response) => response.data));
  }

  deactivateSize(id: number): Observable<void> {
    return this.http
      .patch<ApiResponse<null>>(`${this.sizesUrl}/${id}/deactivate`, {})
      .pipe(map(() => undefined));
  }
}
