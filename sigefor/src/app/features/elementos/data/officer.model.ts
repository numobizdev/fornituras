export interface OfficerSummary {
  id: number;
  nombreCompleto: string;
  placa: string;
  sexoNombre: string | null;
  tipoSangreEtiqueta: string | null;
  municipioNombre: string | null;
  fotoUrl: string | null;
  active: boolean;
}

export interface OfficerDetail {
  id: number;
  nombre: string;
  apellidoPaterno: string;
  apellidoMaterno: string | null;
  nombreCompleto: string;
  placa: string;
  sexoId: number;
  sexoNombre: string | null;
  tipoSangreId: number | null;
  tipoSangreEtiqueta: string | null;
  municipioId: number;
  municipioNombre: string | null;
  /** Enmascarado por el servidor salvo rol autorizado. */
  curp: string | null;
  rfc: string | null;
  piiEnmascarada: boolean;
  fotoUrl: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface OfficerCreateRequest {
  nombre: string;
  apellidoPaterno: string;
  apellidoMaterno?: string | null;
  placa: string;
  sexoId: number;
  tipoSangreId?: number | null;
  municipioId: number;
  curp?: string | null;
  rfc?: string | null;
  fotoUrl?: string | null;
}

/** Ítem de catálogo (sexo, tipo de sangre). */
export interface CatalogItem {
  id: number;
  etiqueta: string;
}

/** Subconjunto de la respuesta paginada de Spring Data que consume la UI. */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
