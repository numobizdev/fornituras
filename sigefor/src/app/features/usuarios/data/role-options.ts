import { UserRole } from '../../../core/models/auth.model';

/**
 * Roles vigentes del sistema (enum `Role` del backend). La ampliación a Supervisor/Almacén/Auditor/
 * Consulta está pendiente de ADR (feature 013), así que aquí solo se ofrecen los dos implementados.
 */
export const ROLE_OPTIONS: ReadonlyArray<{ value: UserRole; label: string }> = [
  { value: 'ADMIN', label: 'Administrador' },
  { value: 'CAPTURISTA', label: 'Capturista' },
];

export const ROLE_LABELS: Record<UserRole, string> = {
  ADMIN: 'Administrador',
  CAPTURISTA: 'Capturista',
};
