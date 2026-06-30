export interface AssignmentSummary {
  id: number;
  equipmentId: number;
  codigoQr: string | null;
  equipmentDescripcion: string | null;
  officerId: number;
  elementoNombre: string | null;
  placa: string | null;
  fechaAsignacion: string;
  fechaDevolucion: string | null;
  vigente: boolean;
}

export interface AssignRequest {
  equipmentId: number;
  officerId: number;
  observaciones?: string | null;
}

export interface ReassignRequest {
  equipmentId: number;
  newOfficerId: number;
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
