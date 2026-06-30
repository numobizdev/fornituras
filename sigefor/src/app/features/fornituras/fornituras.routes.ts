import { Routes } from '@angular/router';

export const FORNITURAS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/fornituras/fornituras.page').then((m) => m.ForniturasPage),
  },
];
