import { NavItem } from '../models/nav-item.model';

export const APP_NAV_ITEMS: NavItem[] = [
  { title: 'Inicio', url: '/inicio', icon: 'home' },
  { title: 'Elementos', url: '/elementos', icon: 'people' },
  { title: 'Fornituras', url: '/fornituras', icon: 'cube' },
  { title: 'Asignación', url: '/asignacion', icon: 'link' },
  { title: 'Traslados', url: '/traslados', icon: 'swap-horizontal' },
  { title: 'Incidencias', url: '/incidencias', icon: 'warning' },
  { title: 'Bajas', url: '/bajas', icon: 'archive' },
  { title: 'Reportes y Estadística', url: '/reportes', icon: 'bar-chart' },
  { title: 'Bitácora de Auditoría', url: '/auditoria', icon: 'shield-checkmark', roles: ['ADMIN'] },
  { title: 'Usuarios', url: '/usuarios', icon: 'person-circle', roles: ['ADMIN'] },
  { title: 'Catálogo de Tipos', url: '/tipos', icon: 'pricetags' },
  { title: 'Almacenes', url: '/almacenes', icon: 'business' },
];
