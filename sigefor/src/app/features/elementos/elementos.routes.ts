import { Routes } from '@angular/router';

export const ELEMENTOS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/elementos/elementos.page').then((m) => m.ElementosPage),
  },
];
