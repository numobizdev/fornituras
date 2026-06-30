import { Routes } from '@angular/router';

export const ASIGNACION_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/asignacion/asignacion.page').then((m) => m.AsignacionPage),
  },
];
