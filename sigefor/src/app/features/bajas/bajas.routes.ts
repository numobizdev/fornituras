import { Routes } from '@angular/router';

export const BAJAS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/bajas/bajas.page').then((m) => m.BajasPage),
  },
  {
    path: 'nueva',
    loadComponent: () => import('./pages/baja-form/baja-form.page').then((m) => m.BajaFormPage),
  },
];
