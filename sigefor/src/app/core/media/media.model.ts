/** Referencia devuelta por el backend al subir una foto (017). */
export interface MediaUploadResponse {
  /** Identificador opaco (UUID) del asset. */
  id: string;
  /** Referencia interna a guardar en `fotoUrl` (p. ej. `/api/v1/media/<uuid>`). */
  url: string;
  /** Content-type final tras el saneo (`image/jpeg`, `image/png`). */
  contentType: string;
}

/** Contexto de la subida: fija si la imagen es PII (elemento) y activa RBAC/gating en el servidor. */
export type MediaContext = 'equipment' | 'equipment_type' | 'officer';
