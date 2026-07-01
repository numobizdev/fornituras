/** Indicadores agregados del tablero (feature 010); coinciden con la respuesta del backend. */
export interface DashboardSummary {
  total: number;
  disponibles: number;
  asignadas: number;
  proximasAVencer: number;
  caducadas: number;
  enMantenimiento: number;
}

/**
 * Definición de cada tarjeta del tablero: qué contador muestra, su etiqueta, su ícono y la variable
 * CSS de color semántico institucional (docs/05-ui-ux.md §3). El color acompaña siempre a la
 * etiqueta, nunca comunica por sí solo (accesibilidad).
 */
export interface DashboardIndicator {
  key: keyof DashboardSummary;
  label: string;
  icon: string;
  colorVar: string;
}

export const DASHBOARD_INDICATORS: readonly DashboardIndicator[] = [
  { key: 'total', label: 'Total de fornituras', icon: 'cube-outline', colorVar: '--status-total' },
  { key: 'disponibles', label: 'Disponibles', icon: 'checkmark-circle-outline', colorVar: '--status-disponible' },
  { key: 'asignadas', label: 'Asignadas', icon: 'person-outline', colorVar: '--status-asignado' },
  { key: 'proximasAVencer', label: 'Próximas a vencer', icon: 'time-outline', colorVar: '--status-proximo-vencer' },
  { key: 'caducadas', label: 'Caducadas', icon: 'alert-circle-outline', colorVar: '--status-caducado' },
  { key: 'enMantenimiento', label: 'En mantenimiento', icon: 'construct-outline', colorVar: '--status-mantenimiento' },
];
