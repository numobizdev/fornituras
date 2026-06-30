export interface SizeSummary {
  id: number;
  etiqueta: string;
  equipmentTypeId: number | null;
  active: boolean;
}

export interface EquipmentTypeSummary {
  id: number;
  nombre: string;
  descripcion: string | null;
  fotoUrl: string | null;
  active: boolean;
}

export interface EquipmentTypeDetail extends EquipmentTypeSummary {
  sizes: SizeSummary[];
  createdAt: string;
  updatedAt: string;
}

export interface EquipmentTypeCreateRequest {
  nombre: string;
  descripcion?: string | null;
  fotoUrl?: string | null;
}

export interface SizeCreateRequest {
  etiqueta: string;
  equipmentTypeId?: number | null;
}

/** Subconjunto de la respuesta paginada de Spring Data que consume la UI. */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
