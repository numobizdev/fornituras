import { Routes } from '@angular/router';

export const LANDING_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/landing-admin/landing-admin.page').then((m) => m.LandingAdminPage),
  },
];
