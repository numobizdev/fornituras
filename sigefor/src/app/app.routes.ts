import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'inicio',
    pathMatch: 'full',
  },
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
];
