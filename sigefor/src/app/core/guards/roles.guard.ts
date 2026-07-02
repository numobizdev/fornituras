import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { UserRole } from '../models/auth.model';
import { AuthService } from '../services/auth.service';

/**
 * Restringe una ruta a los roles de una capacidad de la matriz espejo (021, mínimo
 * privilegio, rechazo por defecto). Complementa la autorización del backend: el servidor
 * ya rechaza al resto, el guard solo evita mostrar pantallas inutilizables.
 */
export const rolesGuard = (roles: ReadonlyArray<UserRole>): CanActivateFn => {
  return async () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    const isAuthenticated = await authService.ensureAuthenticated();
    if (isAuthenticated && authService.hasAnyRole(roles)) {
      return true;
    }

    return router.createUrlTree([isAuthenticated ? '/inicio' : '/login']);
  };
};
