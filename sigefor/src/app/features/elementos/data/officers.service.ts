import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { CATALOG_CODES } from '../../../core/catalog/catalog.model';
import { CatalogService } from '../../../core/catalog/catalog.service';
import { ApiResponse } from '../../../core/models/api-response.model';
import {
  CatalogItem,
  OfficerCreateRequest,
  OfficerDetail,
  OfficerSummary,
  Page,
} from './officer.model';

export interface OfficerListParams {
  q?: string;
  municipio?: string;
  sexoId?: number;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class OfficersService {
  private readonly http = inject(HttpClient);
  private readonly catalog = inject(CatalogService);
  private readonly baseUrl = `${environment.apiUrl}/officers`;

  list(params: OfficerListParams = {}): Observable<Page<OfficerSummary>> {
    let httpParams = new HttpParams();
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, value as string | number);
      }
    }
    return this.http
      .get<ApiResponse<Page<OfficerSummary>>>(this.baseUrl, { params: httpParams })
      .pipe(map((response) => response.data));
  }

  getById(id: number): Observable<OfficerDetail> {
    return this.http
      .get<ApiResponse<OfficerDetail>>(`${this.baseUrl}/${id}`)
      .pipe(map((response) => response.data));
  }

  create(request: OfficerCreateRequest): Observable<OfficerDetail> {
    return this.http
      .post<ApiResponse<OfficerDetail>>(this.baseUrl, request)
      .pipe(map((response) => response.data));
  }

  listSexos(): Observable<CatalogItem[]> {
    return this.catalog
      .listActiveItems(CATALOG_CODES.SEXO)
      .pipe(map((items) => items.map((item) => ({ id: item.id, etiqueta: item.nombre }))));
  }

  listTiposSangre(): Observable<CatalogItem[]> {
    return this.catalog
      .listActiveItems(CATALOG_CODES.TIPO_SANGRE)
      .pipe(map((items) => items.map((item) => ({ id: item.id, etiqueta: item.nombre }))));
  }
}
