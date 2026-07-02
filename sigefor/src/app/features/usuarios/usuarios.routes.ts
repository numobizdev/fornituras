import { Routes } from '@angular/router';
import { rolesGuard } from '../../core/guards/roles.guard';
import { ROLE_POLICY } from '../../core/security/role-policy';

const manageUsersGuard = rolesGuard(ROLE_POLICY.MANAGE_USERS);

export const USUARIOS_ROUTES: Routes = [
  {
    path: '',
    canActivate: [manageUsersGuard],
    loadComponent: () =>
      import('./pages/usuarios/usuarios.page').then((m) => m.UsuariosPage),
  },
  {
    path: 'nuevo',
    canActivate: [manageUsersGuard],
    loadComponent: () =>
      import('./pages/usuario-form/usuario-form.page').then((m) => m.UsuarioFormPage),
  },
  {
    path: ':id',
    canActivate: [manageUsersGuard],
    loadComponent: () =>
      import('./pages/usuario-form/usuario-form.page').then((m) => m.UsuarioFormPage),
  },
];
