import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IonButton, IonIcon } from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import { openOutline } from 'ionicons/icons';
import { LandingSectionPublic, QuickLinkItem } from '../../data/landing.model';

/**
 * Componente presentacional que renderiza una lista de secciones de landing (hero, aviso, accesos
 * rápidos, texto) por tipo. Reutilizado por la cara pública y por el inicio (DRY). Todo el contenido
 * se muestra con interpolación (auto-escape de Angular): nunca `innerHTML`, para evitar XSS (ADR 0015).
 * Los enlaces internos (que empiezan por `/`) usan el router; los externos, un ancla con `rel` seguro.
 */
@Component({
  selector: 'app-landing-sections',
  templateUrl: './landing-sections.component.html',
  styleUrls: ['./landing-sections.component.scss'],
  imports: [RouterLink, IonButton, IonIcon],
})
export class LandingSectionsComponent {
  readonly sections = input.required<LandingSectionPublic[]>();

  constructor() {
    addIcons({ openOutline });
  }

  isInternal(url: string | null | undefined): boolean {
    return !!url && url.startsWith('/');
  }

  trackByOrden(_index: number, section: LandingSectionPublic): number {
    return section.orden;
  }

  trackByLink(_index: number, link: QuickLinkItem): string {
    return link.url;
  }
}
