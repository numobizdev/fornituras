import { Routes } from '@angular/router';
import { adminGuard } from '../../core/guards/admin.guard';

export const USUARIOS_ROUTES: Routes = [
  {
    path: '',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./pages/usuarios/usuarios.page').then((m) => m.UsuariosPage),
  },
  {
    path: 'nuevo',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./pages/usuario-form/usuario-form.page').then((m) => m.UsuarioFormPage),
  },
  {
    path: ':id',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./pages/usuario-form/usuario-form.page').then((m) => m.UsuarioFormPage),
  },
];
