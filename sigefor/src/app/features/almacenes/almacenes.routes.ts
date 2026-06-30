import { Routes } from '@angular/router';

export const ALMACENES_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/almacenes/almacenes.page').then((m) => m.AlmacenesPage),
  },
  {
    path: 'nuevo',
    loadComponent: () =>
      import('./pages/almacen-form/almacen-form.page').then((m) => m.AlmacenFormPage),
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./pages/almacen-form/almacen-form.page').then((m) => m.AlmacenFormPage),
  },
];
