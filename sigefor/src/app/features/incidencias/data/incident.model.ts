export type IncidentType = 'DANO' | 'FALLA' | 'EXTRAVIO' | 'MANTENIMIENTO';
export type IncidentStatus = 'ABIERTA' | 'EN_PROCESO' | 'RESUELTA' | 'CERRADA';
export type ExpiryStatus = 'VIGENTE' | 'PROXIMA_A_VENCER' | 'CADUCADA';

/** Tipos de incidencia con etiqueta legible. */
export const INCIDENT_TYPES: Record<IncidentType, { label: string }> = {
  DANO: { label: 'Daño' },
  FALLA: { label: 'Falla' },
  EXTRAVIO: { label: 'Extravío' },
  MANTENIMIENTO: { label: 'Mantenimiento' },
};

/** Estados de incidencia con etiqueta y color semántico (Ionic); el color acompaña siempre a la etiqueta. */
export const INCIDENT_STATUSES: Record<IncidentStatus, { label: string; color: string }> = {
  ABIERTA: { label: 'Abierta', color: 'warning' },
  EN_PROCESO: { label: 'En proceso', color: 'tertiary' },
  RESUELTA: { label: 'Resuelta', color: 'success' },
  CERRADA: { label: 'Cerrada', color: 'medium' },
};

/** Severidad de las alertas de vigencia derivadas (color + etiqueta, nunca solo color). */
export const EXPIRY_ALERTS: Record<'PROXIMA_A_VENCER' | 'CADUCADA', { label: string; color: string }> = {
  CADUCADA: { label: 'Caducada', color: 'danger' },
  PROXIMA_A_VENCER: { label: 'Próxima a vencer', color: 'warning' },
};

export interface IncidentSummary {
  id: number;
  equipmentId: number;
  equipmentCodigo: string | null;
  tipo: IncidentType;
  descripcion: string;
  estado: IncidentStatus;
  fechaReporte: string;
  fechaResolucion: string | null;
}

export interface IncidentCreateRequest {
  equipmentId: number;
  tipo: IncidentType;
  descripcion: string;
}

export interface IncidentUpdateRequest {
  estado: IncidentStatus;
}

export interface AlertItem {
  equipmentId: number;
  equipmentCodigo: string | null;
  descripcion: string | null;
  fechaVencimiento: string;
  expiryStatus: ExpiryStatus;
}

/** Subconjunto de la respuesta paginada de Spring Data que consume la UI. */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
