/** Motivo de baja del catálogo (para poblar el selector del formulario). */
export interface DecommissionReasonItem {
  id: number;
  nombre: string;
}

/** Fila del listado de fornituras dadas de baja. Sin PII (el responsable es un id de usuario). */
export interface DecommissionSummary {
  id: number;
  equipmentId: number;
  equipmentCodigo: string | null;
  descripcion: string | null;
  tipoNombre: string | null;
  motivoId: number;
  motivoNombre: string | null;
  fecha: string;
  responsable: number | null;
  observaciones: string | null;
}

/** Petición de baja: la fornitura se identifica por código (resuelto server-side) y el motivo por id. */
export interface DecommissionRequest {
  codigo: string;
  motivoId: number;
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
