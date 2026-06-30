import { Routes } from '@angular/router';

export const FORNITURAS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/fornituras/fornituras.page').then((m) => m.ForniturasPage),
  },
  {
    path: 'nuevo',
    loadComponent: () =>
      import('./pages/fornitura-form/fornitura-form.page').then((m) => m.FornituraFormPage),
  },
  {
    path: 'lote',
    loadComponent: () =>
      import('./pages/fornitura-lote/fornitura-lote.page').then((m) => m.FornituraLotePage),
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./pages/fornitura-form/fornitura-form.page').then((m) => m.FornituraFormPage),
  },
];
