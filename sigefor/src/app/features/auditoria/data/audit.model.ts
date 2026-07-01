/** Fila de la bitácora de auditoría (012). Solo trazabilidad; sin PII en claro. */
export interface AuditLogSummary {
  id: number;
  usuarioId: number | null;
  actor: string | null;
  accion: string;
  entidad: string | null;
  entidadId: number | null;
  occurredAt: string;
  ip: string | null;
  evidencia: string | null;
}

/** Filtros de la consulta de bitácora; todos opcionales. */
export interface AuditFilter {
  actor?: string;
  accion?: string;
  entidad?: string;
  desde?: string;
  hasta?: string;
}

/** Subconjunto de la respuesta paginada de Spring Data que consume la UI. */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
