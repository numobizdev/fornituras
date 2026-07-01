import { UserRole } from '../../../core/models/auth.model';

export type { UserRole };

/** Ficha de usuario que expone la API (nunca incluye la contraseña ni el token). */
export interface UserSummary {
  id: number;
  name: string;
  email: string;
  role: UserRole;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

/** Alta de usuario: el backend crea la cuenta pendiente y envía un código de activación por correo. */
export interface UserCreateRequest {
  name: string;
  email: string;
  role: UserRole;
}

/** Edición de datos básicos. El email es identidad de login y no se edita aquí. */
export interface UserUpdateRequest {
  name: string;
}

/** Subconjunto de la respuesta paginada de Spring Data que consume la UI. */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
