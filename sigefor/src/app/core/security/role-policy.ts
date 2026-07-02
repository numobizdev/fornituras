import { UserRole } from '../models/auth.model';

/**
 * Matriz RBAC de la interfaz — ESPEJO 1:1 de la matriz del backend
 * (`fornituras-api-dotnet/src/Fornituras.Api/Security/RolePolicy.cs`, ADR 0013).
 *
 * Regla de sincronización: cualquier cambio en RolePolicy.cs DEBE replicarse aquí
 * (y viceversa nunca: el backend es la autoridad; la UI solo refleja).
 * La UI nunca muestra una acción que el servidor rechazaría al rol.
 */
export const ROLE_POLICY = {
  WRITE_INVENTORY: ['ADMIN', 'ALMACEN', 'CAPTURISTA'],
  WRITE_TRANSFERS: ['ADMIN', 'SUPERVISOR', 'ALMACEN', 'CAPTURISTA'],
  WRITE_OPERATIONS: ['ADMIN', 'SUPERVISOR', 'CAPTURISTA'],
  AUTHORIZE_DECOMMISSION: ['ADMIN', 'SUPERVISOR'],
  WRITE_OFFICERS: ['ADMIN', 'SUPERVISOR', 'CAPTURISTA'],
  MANAGE_CONFIG: ['ADMIN'],
  MANAGE_LANDING: ['ADMIN'],
  MANAGE_USERS: ['ADMIN'],
  READ_AUDIT: ['ADMIN', 'AUDITOR'],
} as const satisfies Record<string, ReadonlyArray<UserRole>>;
