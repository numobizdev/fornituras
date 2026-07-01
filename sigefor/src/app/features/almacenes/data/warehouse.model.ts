/** Vista de listado: solo campos no sensibles. La devuelve la API a cualquier rol autenticado. */
export interface WarehouseSummary {
  id: number;
  codigo: string;
  nombre: string;
  /** FK al valor del catálogo TIPO_ALMACEN (ADR 0007) y su nombre resuelto para mostrar. */
  tipoItemId: number;
  tipoNombre: string | null;
  active: boolean;
}

/** Ficha completa con campos sensibles (ubicación, responsable, contacto). Solo ADMIN. */
export interface WarehouseDetail {
  id: number;
  codigo: string;
  nombre: string;
  tipoItemId: number;
  tipoNombre: string | null;
  /** Municipio/estado como texto libre (ADR 0007). */
  municipio: string | null;
  estado: string | null;
  direccion: string | null;
  cp: string | null;
  latitud: number | null;
  longitud: number | null;
  responsableId: number | null;
  telefono: string | null;
  emailContacto: string | null;
  capacidad: number | null;
  observaciones: string | null;
  active: boolean;
  ocupacion: number;
  porcentajeOcupacion: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface WarehouseCreateRequest {
  codigo: string;
  nombre: string;
  tipoItemId: number;
  municipio?: string | null;
  estado?: string | null;
  direccion?: string | null;
  cp?: string | null;
  latitud?: number | null;
  longitud?: number | null;
  responsableId?: number | null;
  telefono?: string | null;
  emailContacto?: string | null;
  capacidad?: number | null;
  observaciones?: string | null;
}

/** Subconjunto de la respuesta paginada de Spring Data que consume la UI. */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
