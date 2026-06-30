import { Routes } from '@angular/router';

export const ELEMENTOS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/elementos/elementos.page').then((m) => m.ElementosPage),
  },
  {
    path: 'nuevo',
    loadComponent: () =>
      import('./pages/elemento-form/elemento-form.page').then((m) => m.ElementoFormPage),
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./pages/elemento-form/elemento-form.page').then((m) => m.ElementoFormPage),
  },
];
