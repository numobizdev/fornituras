/** Claves de los catálogos de sistema (ADR 0007). Espejo de CatalogCodes del backend. */
export const CATALOG_CODES = {
  TIPO_PRENDA: 'TIPO_PRENDA',
  TALLA: 'TALLA',
  TIPO_ALMACEN: 'TIPO_ALMACEN',
} as const;

export type CatalogCode = (typeof CATALOG_CODES)[keyof typeof CATALOG_CODES];

/** Cabecera de catálogo administrable. */
export interface CatalogSummary {
  id: number;
  code: string;
  nombre: string;
  descripcion: string | null;
  system: boolean;
  active: boolean;
}

/** Valor de un catálogo (con su catálogo y padre para jerarquías, p. ej. talla-por-tipo). */
export interface CatalogItemSummary {
  id: number;
  catalogId: number;
  catalogCode: string;
  code: string | null;
  nombre: string;
  descripcion: string | null;
  fotoUrl: string | null;
  parentItemId: number | null;
  orden: number | null;
  active: boolean;
}

export interface CatalogItemCreateRequest {
  nombre: string;
  code?: string | null;
  descripcion?: string | null;
  fotoUrl?: string | null;
  parentItemId?: number | null;
  orden?: number | null;
}

/** Subconjunto de la respuesta paginada de Spring Data que consume la UI. */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
