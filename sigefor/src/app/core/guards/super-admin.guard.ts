import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/** Ruta de inicio del módulo QR (rol SUPER_ADMIN). */
export const QR_LOTES_HOME = '/qr-lotes';

/**
 * Restringe rutas al rol SUPER_ADMIN (módulo de lotes QR, ADR 0021).
 * Complementa la autorización del backend.
 */
export const superAdminGuard: CanActivateFn = async () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const isAuthenticated = await authService.ensureAuthenticated();
  if (!isAuthenticated) {
    return router.createUrlTree(['/login']);
  }

  if (authService.hasRole('SUPER_ADMIN')) {
    return true;
  }

  return router.createUrlTree(['/inicio']);
};
