import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { MediaContext, MediaUploadResponse } from './media.model';

/**
 * Cliente del módulo de fotos (017). Sube la imagen como `multipart/form-data` (el interceptor de auth
 * añade el Bearer token) y resuelve la descarga autenticada como `blob`, ya que `<img src>` no envía
 * la cabecera `Authorization`. Nunca almacena datos sensibles: solo referencias internas opacas.
 */
@Injectable({ providedIn: 'root' })
export class MediaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/media`;

  /** Sube una imagen y devuelve la referencia interna a guardar en `fotoUrl`. */
  upload(file: Blob, context: MediaContext, fileName = 'photo'): Observable<MediaUploadResponse> {
    const form = new FormData();
    form.append('image', file, fileName);
    form.append('context', context);
    return this.http
      .post<ApiResponse<MediaUploadResponse>>(this.baseUrl, form)
      .pipe(map((response) => response.data));
  }

  /** Descarga la imagen descifrada como blob (con token vía interceptor) para un `<img>` seguro. */
  download(id: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${id}`, { responseType: 'blob' });
  }

  /** Elimina una foto (purga el objeto en el servidor conforme a retención/ARCO). */
  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  /**
   * Extrae el id opaco de una referencia interna (`/api/v1/media/<uuid>` o el propio uuid). Devuelve
   * `null` si es una URL externa previa (transición, FR-013) que debe cargarse tal cual en el `<img>`.
   */
  resolveInternalId(reference: string | null | undefined): string | null {
    if (!reference) {
      return null;
    }
    const trimmed = reference.trim();
    const match = trimmed.match(/\/media\/([^/?#]+)$/);
    if (match) {
      return match[1];
    }
    // ¿Es un UUID desnudo? (referencia mínima, sin ruta)
    if (/^[0-9a-fA-F-]{36}$/.test(trimmed)) {
      return trimmed;
    }
    return null;
  }
}
