import { Routes } from '@angular/router';

export const TIPOS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/tipos/tipos.page').then((m) => m.TiposPage),
  },
  {
    path: 'nuevo',
    loadComponent: () =>
      import('./pages/tipo-form/tipo-form.page').then((m) => m.TipoFormPage),
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./pages/tipo-form/tipo-form.page').then((m) => m.TipoFormPage),
  },
];
