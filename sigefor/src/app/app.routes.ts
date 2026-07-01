import { Routes } from '@angular/router';
import { adminGuard } from './core/guards/admin.guard';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';

export const routes: Routes = [
  {
    // Raíz: landing pública para visitantes; un usuario autenticado se redirige a /inicio (FR-014/015).
    path: '',
    pathMatch: 'full',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/landing/pages/public-landing/public-landing.page').then(
        (m) => m.PublicLandingPage,
      ),
  },
  {
    path: '',
    loadChildren: () =>
      import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },
  {
    path: '',
    canActivate: [authGuard],
    children: [
      {
        path: 'inicio',
        loadChildren: () =>
          import('./features/inicio/inicio.routes').then((m) => m.INICIO_ROUTES),
      },
      {
        path: 'elementos',
        loadChildren: () =>
          import('./features/elementos/elementos.routes').then((m) => m.ELEMENTOS_ROUTES),
      },
      {
        path: 'fornituras',
        loadChildren: () =>
          import('./features/fornituras/fornituras.routes').then((m) => m.FORNITURAS_ROUTES),
      },
      {
        path: 'asignacion',
        loadChildren: () =>
          import('./features/asignacion/asignacion.routes').then((m) => m.ASIGNACION_ROUTES),
      },
      {
        path: 'tipos',
        loadChildren: () =>
          import('./features/tipos/tipos.routes').then((m) => m.TIPOS_ROUTES),
      },
      {
        path: 'almacenes',
        loadChildren: () =>
          import('./features/almacenes/almacenes.routes').then((m) => m.ALMACENES_ROUTES),
      },
      {
        path: 'traslados',
        loadChildren: () =>
          import('./features/traslados/traslados.routes').then((m) => m.TRASLADOS_ROUTES),
      },
      {
        path: 'incidencias',
        loadChildren: () =>
          import('./features/incidencias/incidencias.routes').then((m) => m.INCIDENCIAS_ROUTES),
      },
      {
        path: 'bajas',
        loadChildren: () =>
          import('./features/bajas/bajas.routes').then((m) => m.BAJAS_ROUTES),
      },
      {
        path: 'reportes',
        loadChildren: () =>
          import('./features/reportes/reportes.routes').then((m) => m.REPORTES_ROUTES),
      },
      {
        path: 'auditoria',
        loadChildren: () =>
          import('./features/auditoria/auditoria.routes').then((m) => m.AUDITORIA_ROUTES),
      },
      {
        path: 'usuarios',
        loadChildren: () =>
          import('./features/usuarios/usuarios.routes').then((m) => m.USUARIOS_ROUTES),
      },
      {
        path: 'landing-admin',
        canActivate: [adminGuard],
        loadChildren: () =>
          import('./features/landing/landing.routes').then((m) => m.LANDING_ROUTES),
      },
    ],
  },
  {
    // Cualquier ruta desconocida vuelve a la raíz: los invitados aterrizan en la landing (no en el
    // shell autenticado) y los usuarios con sesión, en su inicio (FR-015).
    path: '**',
    redirectTo: '',
  },
];
