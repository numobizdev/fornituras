export type EquipmentStatus =
  | 'DISPONIBLE'
  | 'ASIGNADA'
  | 'EN_MANTENIMIENTO'
  | 'EN_TRASLADO'
  | 'EXTRAVIADA'
  | 'BAJA_DEFINITIVA';

export type ExpiryStatus = 'VIGENTE' | 'PROXIMA_A_VENCER' | 'CADUCADA';

/** Estados operativos con etiqueta legible y color semántico (Ionic). */
export const EQUIPMENT_STATUSES: { value: EquipmentStatus; label: string; color: string }[] = [
  { value: 'DISPONIBLE', label: 'Disponible', color: 'success' },
  { value: 'ASIGNADA', label: 'Asignada', color: 'primary' },
  { value: 'EN_MANTENIMIENTO', label: 'En mantenimiento', color: 'warning' },
  { value: 'EN_TRASLADO', label: 'En traslado', color: 'tertiary' },
  { value: 'EXTRAVIADA', label: 'Extraviada', color: 'danger' },
  { value: 'BAJA_DEFINITIVA', label: 'Baja definitiva', color: 'medium' },
];

export const EXPIRY_STATUSES: Record<ExpiryStatus, { label: string; color: string }> = {
  VIGENTE: { label: 'Vigente', color: 'success' },
  PROXIMA_A_VENCER: { label: 'Próxima a vencer', color: 'warning' },
  CADUCADA: { label: 'Caducada', color: 'danger' },
};

export interface EquipmentSummary {
  id: number;
  codigoQr: string;
  descripcion: string | null;
  tipoNombre: string | null;
  tallaEtiqueta: string | null;
  almacenNombre: string | null;
  status: EquipmentStatus;
  vigencia: ExpiryStatus | null;
  fechaVencimiento: string | null;
}

export interface EquipmentDetail {
  id: number;
  codigoQr: string;
  equipmentTypeId: number;
  tipoNombre: string | null;
  sizeId: number | null;
  tallaEtiqueta: string | null;
  warehouseId: number;
  almacenNombre: string | null;
  status: EquipmentStatus;
  vigencia: ExpiryStatus | null;
  descripcion: string | null;
  marca: string | null;
  modelo: string | null;
  nivelBalistico: string | null;
  numeroInventario: string | null;
  fechaFabricacion: string | null;
  fechaAdquisicion: string | null;
  vidaUtilMeses: number | null;
  fechaVencimiento: string | null;
  observaciones: string | null;
  fotoUrl: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface EquipmentCreateRequest {
  codigoQr: string;
  equipmentTypeId: number;
  sizeId?: number | null;
  warehouseId: number;
  descripcion?: string | null;
  marca?: string | null;
  modelo?: string | null;
  nivelBalistico?: string | null;
  numeroInventario?: string | null;
  fechaFabricacion?: string | null;
  fechaAdquisicion?: string | null;
  vidaUtilMeses?: number | null;
  fechaVencimiento?: string | null;
  observaciones?: string | null;
  fotoUrl?: string | null;
}

export interface BatchCreateRequest {
  equipmentTypeId: number;
  sizeId?: number | null;
  warehouseId: number;
  descripcion?: string | null;
  marca?: string | null;
  modelo?: string | null;
  nivelBalistico?: string | null;
  fechaFabricacion?: string | null;
  fechaAdquisicion?: string | null;
  vidaUtilMeses?: number | null;
  fechaVencimiento?: string | null;
  observaciones?: string | null;
  codigos: string[];
}

/** Subconjunto de la respuesta paginada de Spring Data que consume la UI. */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
