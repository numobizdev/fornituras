import { Routes } from '@angular/router';

export const TRASLADOS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/traslados/traslados.page').then((m) => m.TrasladosPage),
  },
  {
    path: 'nuevo',
    loadComponent: () =>
      import('./pages/traslado-form/traslado-form.page').then((m) => m.TrasladoFormPage),
  },
];
