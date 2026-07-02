import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../core/models/api-response.model';
import {
  GenerateQrRequest,
  LoteQrSummary,
  ReprintQrRequest,
} from './qr-lote.model';

/**
 * Acceso a la API de lotes QR (/api/v1/qr). Solo SUPER_ADMIN (backend + guards).
 */
@Injectable({ providedIn: 'root' })
export class QrLotesService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/qr`;

  listLotes(): Observable<LoteQrSummary[]> {
    return this.http
      .get<ApiResponse<LoteQrSummary[]>>(`${this.baseUrl}/lotes`)
      .pipe(map((response) => response.data));
  }

  getLote(id: number): Observable<LoteQrSummary> {
    return this.http
      .get<ApiResponse<LoteQrSummary>>(`${this.baseUrl}/lotes/${id}`)
      .pipe(map((response) => response.data));
  }

  generateLote(request: GenerateQrRequest): Observable<LoteQrSummary> {
    return this.http
      .post<ApiResponse<LoteQrSummary>>(`${this.baseUrl}/lotes`, request)
      .pipe(map((response) => response.data));
  }

  downloadPdfOriginal(id: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/lotes/${id}/pdf`, { responseType: 'blob' });
  }

  downloadZipOriginal(id: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/lotes/${id}/zip`, { responseType: 'blob' });
  }

  downloadPdfReprint(id: number, form: ReprintQrRequest): Observable<Blob> {
    return this.http.post(`${this.baseUrl}/lotes/${id}/export/pdf`, form, {
      responseType: 'blob',
    });
  }

  downloadZipReprint(id: number, form: ReprintQrRequest): Observable<Blob> {
    return this.http.post(`${this.baseUrl}/lotes/${id}/export/zip`, form, {
      responseType: 'blob',
    });
  }
}
