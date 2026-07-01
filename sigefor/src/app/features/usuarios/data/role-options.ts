import { UserRole } from '../../../core/models/auth.model';

/**
 * Roles del sistema (enum `Role` del backend). La matriz rol→permisos se fija en el ADR 0013
 * (mínimo privilegio) y se propaga en el backend vía `RolePolicy`.
 */
export const ROLE_OPTIONS: ReadonlyArray<{ value: UserRole; label: string }> = [
  { value: 'ADMIN', label: 'Administrador' },
  { value: 'SUPERVISOR', label: 'Supervisor' },
  { value: 'ALMACEN', label: 'Almacén' },
  { value: 'AUDITOR', label: 'Auditor' },
  { value: 'CAPTURISTA', label: 'Capturista' },
];

export const ROLE_LABELS: Record<UserRole, string> = {
  ADMIN: 'Administrador',
  SUPERVISOR: 'Supervisor',
  ALMACEN: 'Almacén',
  AUDITOR: 'Auditor',
  CAPTURISTA: 'Capturista',
};
