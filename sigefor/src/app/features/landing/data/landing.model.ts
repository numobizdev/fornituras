export type LandingScope = 'PUBLIC' | 'HOME';

export type LandingSectionType = 'HERO' | 'ANNOUNCEMENT' | 'QUICK_LINKS' | 'RICH_TEXT';

export interface QuickLinkItem {
  label: string;
  url: string;
  icon?: string | null;
}

/** Proyección de solo lectura para las caras pública y de inicio (sin id/scope internos, sin PII). */
export interface LandingSectionPublic {
  type: LandingSectionType;
  titulo: string | null;
  subtitulo: string | null;
  cuerpo: string | null;
  imagenUrl: string | null;
  ctaLabel: string | null;
  ctaUrl: string | null;
  orden: number;
  quickLinks: QuickLinkItem[];
}

/** Proyección completa para el editor de ADMIN. */
export interface LandingSectionAdmin {
  id: number;
  scope: LandingScope;
  type: LandingSectionType;
  titulo: string | null;
  subtitulo: string | null;
  cuerpo: string | null;
  imagenUrl: string | null;
  ctaLabel: string | null;
  ctaUrl: string | null;
  orden: number;
  active: boolean;
  quickLinks: QuickLinkItem[];
  createdAt: string;
  updatedAt: string;
}

/** Cuerpo de alta/edición de una sección. */
export interface LandingSectionRequest {
  scope: LandingScope;
  type: LandingSectionType;
  titulo?: string | null;
  subtitulo?: string | null;
  cuerpo?: string | null;
  imagenUrl?: string | null;
  ctaLabel?: string | null;
  ctaUrl?: string | null;
  orden: number;
  quickLinks?: QuickLinkItem[] | null;
}

export interface ReorderItem {
  id: number;
  orden: number;
}

export interface ReorderRequest {
  items: ReorderItem[];
}
