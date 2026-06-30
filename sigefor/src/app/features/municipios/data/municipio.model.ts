/** Catálogo geográfico de municipios. Reutilizable por almacenes (005) y elementos (003). */
export interface MunicipioSummary {
  id: number;
  nombre: string;
  active: boolean;
}

/** Subconjunto de la respuesta paginada de Spring Data que consume la UI. */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
