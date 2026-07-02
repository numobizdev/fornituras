import { NavItem } from '../models/nav-item.model';
import { ROLE_POLICY } from '../security/role-policy';

// Los `roles` de cada entrada derivan de la matriz espejo (021/FR-002..FR-004):
// nada de arreglos literales por ítem.
export const APP_NAV_ITEMS: NavItem[] = [
  { title: 'Inicio', url: '/inicio', icon: 'home' },
  { title: 'Elementos', url: '/elementos', icon: 'people' },
  { title: 'Fornituras', url: '/fornituras', icon: 'cube' },
  { title: 'Asignación', url: '/asignacion', icon: 'link' },
  { title: 'Traslados', url: '/traslados', icon: 'swap-horizontal' },
  { title: 'Incidencias', url: '/incidencias', icon: 'warning' },
  { title: 'Bajas', url: '/bajas', icon: 'archive' },
  { title: 'Reportes y Estadística', url: '/reportes', icon: 'bar-chart' },
  {
    title: 'Bitácora de Auditoría',
    url: '/auditoria',
    icon: 'shield-checkmark',
    roles: [...ROLE_POLICY.READ_AUDIT],
  },
  { title: 'Usuarios', url: '/usuarios', icon: 'person-circle', roles: [...ROLE_POLICY.MANAGE_USERS] },
  {
    title: 'Configurar landing',
    url: '/landing-admin',
    icon: 'megaphone',
    roles: [...ROLE_POLICY.MANAGE_LANDING],
  },
  {
    title: 'Lotes QR',
    url: '/qr-lotes',
    icon: 'qr-code',
    roles: [...ROLE_POLICY.MANAGE_QR_LOTES],
  },
  { title: 'Catálogo de Tipos', url: '/tipos', icon: 'pricetags' },
  { title: 'Almacenes', url: '/almacenes', icon: 'business' },
];
