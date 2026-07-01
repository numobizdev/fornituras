import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Restringe una ruta a administradores (mínimo privilegio, rechazo por defecto). Complementa la
 * autorización del backend: aunque el servidor ya rechaza a los no-admin, el guard evita mostrar
 * pantallas que el usuario no puede usar y redirige a inicio. Ver feature 013 (RBAC).
 */
export const adminGuard: CanActivateFn = async () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const isAuthenticated = await authService.ensureAuthenticated();
  if (isAuthenticated && authService.hasRole('ADMIN')) {
    return true;
  }

  return router.createUrlTree([isAuthenticated ? '/inicio' : '/login']);
};
