import { UserRole } from '../models/auth.model';

const ROLE_LABELS: Record<UserRole, string> = {
  ADMIN: 'Administrador',
  SUPER_ADMIN: 'Super Admin QR',
  SUPERVISOR: 'Supervisor',
  ALMACEN: 'Almacén',
  AUDITOR: 'Auditor',
  CAPTURISTA: 'Capturista',
};

/** Etiqueta es-MX del rol; cadena vacía si el rol no es reconocido (rechazo por defecto). */
export function roleLabel(role: UserRole | null | undefined): string {
  return role ? (ROLE_LABELS[role] ?? '') : '';
}
