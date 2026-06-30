import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../core/models/api-response.model';
import { MunicipioSummary, Page } from './municipio.model';

@Injectable({ providedIn: 'root' })
export class MunicipiosService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/municipios`;

  /** Lista municipios para usarlos como selector (por defecto solo activos). */
  list(options: { active?: boolean; size?: number } = {}): Observable<MunicipioSummary[]> {
    let params = new HttpParams().set('size', options.size ?? 500);
    if (options.active !== undefined) {
      params = params.set('active', options.active);
    }
    return this.http
      .get<ApiResponse<Page<MunicipioSummary>>>(this.baseUrl, { params })
      .pipe(map((response) => response.data.content));
  }
}
