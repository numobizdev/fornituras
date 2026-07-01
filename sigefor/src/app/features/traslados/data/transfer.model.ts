export type TransferStatus = 'ENVIADO' | 'RECIBIDO' | 'CANCELADO';

/** Estados de traslado con etiqueta legible y color semántico (Ionic). */
export const TRANSFER_STATUSES: Record<TransferStatus, { label: string; color: string }> = {
  ENVIADO: { label: 'Enviado', color: 'warning' },
  RECIBIDO: { label: 'Recibido', color: 'success' },
  CANCELADO: { label: 'Cancelado', color: 'medium' },
};

export interface TransferSummary {
  id: number;
  origenId: number;
  origenNombre: string | null;
  destinoId: number;
  destinoNombre: string | null;
  status: TransferStatus;
  fechaEnvio: string;
  fechaRecepcion: string | null;
  itemCount: number;
}

export interface TransferItemDetail {
  equipmentId: number;
  codigoQr: string | null;
  descripcion: string | null;
}

export interface TransferDetail {
  id: number;
  origenId: number;
  origenNombre: string | null;
  destinoId: number;
  destinoNombre: string | null;
  status: TransferStatus;
  fechaEnvio: string;
  fechaRecepcion: string | null;
  observaciones: string | null;
  items: TransferItemDetail[];
}

export interface TransferCreateRequest {
  origenId: number;
  destinoId: number;
  equipmentIds: number[];
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
