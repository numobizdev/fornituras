import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { QR_LOTES_HOME } from './super-admin.guard';

/**
 * Bloquea al rol SUPER_ADMIN (QR-only) fuera del módulo de lotes QR.
 * Aplicar junto con authGuard en el shell autenticado.
 */
export const operationalGuard: CanActivateFn = async (_route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const isAuthenticated = await authService.ensureAuthenticated();
  if (!isAuthenticated) {
    return router.createUrlTree(['/login']);
  }

  if (authService.hasRole('SUPER_ADMIN')) {
    const path = state.url.split('?')[0];
    if (path.startsWith(QR_LOTES_HOME)) {
      return true;
    }
    return router.createUrlTree([QR_LOTES_HOME]);
  }

  return true;
};
