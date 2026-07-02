
import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import {
  IonApp,
  IonContent,
  IonIcon,
  IonItem,
  IonLabel,
  IonList,
  IonListHeader,
  IonMenu,
  IonMenuToggle,
  IonNote,
  IonRouterLink,
  IonRouterOutlet,
  IonSplitPane,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import {
  archiveOutline,
  archiveSharp,
  barChartOutline,
  barChartSharp,
  businessOutline,
  businessSharp,
  shieldCheckmarkOutline,
  shieldCheckmarkSharp,
  cubeOutline,
  cubeSharp,
  homeOutline,
  homeSharp,
  linkOutline,
  linkSharp,
  logOutOutline,
  logOutSharp,
  megaphoneOutline,
  megaphoneSharp,
  peopleOutline,
  peopleSharp,
  personCircleOutline,
  personCircleSharp,
  pricetagsOutline,
  pricetagsSharp,
  qrCodeOutline,
  qrCodeSharp,
  swapHorizontalOutline,
  swapHorizontalSharp,
  warningOutline,
  warningSharp,
} from 'ionicons/icons';
import { APP_NAV_ITEMS } from './core/constants/app-navigation';
import { roleLabel } from './core/constants/role-labels';
import { NavItem } from './core/models/nav-item.model';
import { AuthService } from './core/services/auth.service';

@Component({
  selector: 'app-root',
  templateUrl: 'app.component.html',
  styleUrls: ['app.component.scss'],
  imports: [
    RouterLink,
    RouterLinkActive,
    IonApp,
    IonSplitPane,
    IonMenu,
    IonContent,
    IonList,
    IonListHeader,
    IonMenuToggle,
    IonItem,
    IonIcon,
    IonLabel,
    IonNote,
    IonRouterLink,
    IonRouterOutlet,
  ],
})
export class AppComponent {
  private readonly authService = inject(AuthService);

  public readonly currentUser = this.authService.currentUser;

  /** Etiqueta es-MX del rol conectado, visible en el encabezado del menú (021/FR-005). */
  public readonly currentRoleLabel = computed(() => roleLabel(this.currentUser()?.role));

  /** Menú filtrado por rol: SUPER_ADMIN solo ve Lotes QR; el resto por matriz espejo (mínimo privilegio). */
  public readonly appPages = computed<NavItem[]>(() => {
    const role = this.currentUser()?.role;
    if (role === 'SUPER_ADMIN') {
      return APP_NAV_ITEMS.filter(
        (item) => item.roles?.includes('SUPER_ADMIN') ?? false,
      );
    }
    return APP_NAV_ITEMS.filter((item) => !item.roles || (role != null && item.roles.includes(role)));
  });

  /**
   * El shell autenticado (menú lateral) se monta SOLO con sesión válida, derivándolo del estado de
   * sesión y no de la URL (FR-016). Un visitante sin sesión (landing, login, recuperación) nunca ve el
   * menú. La sesión se restaura en el arranque (`provideAppInitializer`), así que es fiable al renderizar.
   */
  public readonly showMenu = computed(() => this.authService.isAuthenticated());

  constructor() {
    addIcons({
      archiveOutline,
      archiveSharp,
      barChartOutline,
      barChartSharp,
      homeOutline,
      homeSharp,
      peopleOutline,
      peopleSharp,
      personCircleOutline,
      personCircleSharp,
      cubeOutline,
      cubeSharp,
      linkOutline,
      linkSharp,
      logOutOutline,
      logOutSharp,
      megaphoneOutline,
      megaphoneSharp,
      pricetagsOutline,
      pricetagsSharp,
      qrCodeOutline,
      qrCodeSharp,
      businessOutline,
      businessSharp,
      shieldCheckmarkOutline,
      shieldCheckmarkSharp,
      swapHorizontalOutline,
      swapHorizontalSharp,
      warningOutline,
      warningSharp,
    });
  }

  async logout(): Promise<void> {
    await this.authService.logout();
  }
}
