/** Totales de la vista de control (011); coinciden con el tablero (010). */
export interface ReportTotals {
  totalFornituras: number;
  disponibles: number;
  asignadas: number;
  enMantenimiento: number;
  conIncidencia: number;
  baja: number;
  totalElementos: number;
}

/**
 * Fila de asignación activa. `curp`/`rfc` llegan enmascaradas salvo para roles autorizados;
 * `piiMasked` indica si vienen ocultas (el servidor decide, nunca el cliente).
 */
export interface ActiveAssignmentRow {
  assignmentId: number;
  equipmentId: number;
  codigoQr: string | null;
  equipmentDescripcion: string | null;
  officerId: number;
  elementoNombre: string | null;
  placa: string | null;
  curp: string | null;
  rfc: string | null;
  municipio: string | null;
  estado: string | null;
  piiMasked: boolean;
  fechaAsignacion: string;
}

/** Filtros del reporte de asignaciones activas; todos opcionales. */
export interface ActiveAssignmentFilter {
  qr?: string;
  nombre?: string;
  rfc?: string;
  placa?: string;
  curp?: string;
  municipio?: string;
}

/** Subconjunto de la respuesta paginada de Spring Data que consume la UI. */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
