import { Routes } from '@angular/router';

export const INCIDENCIAS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/incidencias/incidencias.page').then((m) => m.IncidenciasPage),
  },
  {
    path: 'nueva',
    loadComponent: () =>
      import('./pages/incidencia-form/incidencia-form.page').then((m) => m.IncidenciaFormPage),
  },
];
