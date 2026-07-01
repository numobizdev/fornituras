
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { filter, map, startWith } from 'rxjs/operators';
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
  peopleOutline,
  peopleSharp,
  pricetagsOutline,
  pricetagsSharp,
  swapHorizontalOutline,
  swapHorizontalSharp,
  warningOutline,
  warningSharp,
} from 'ionicons/icons';
import { APP_NAV_ITEMS } from './core/constants/app-navigation';
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
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  public readonly currentUser = this.authService.currentUser;

  /** Menú filtrado por rol: las entradas con `roles` solo se muestran a esos roles (mínimo privilegio). */
  public readonly appPages = computed<NavItem[]>(() => {
    const role = this.currentUser()?.role;
    return APP_NAV_ITEMS.filter((item) => !item.roles || (role != null && item.roles.includes(role)));
  });

  private readonly currentUrl = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      map((event) => event.urlAfterRedirects),
      startWith(this.router.url),
    ),
    { initialValue: this.router.url },
  );

  public readonly showMenu = computed(() => {
    const url = this.currentUrl();
    return !url.startsWith('/login') &&
      !url.startsWith('/forgot-password') &&
      !url.startsWith('/reset-password');
  });

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
      cubeOutline,
      cubeSharp,
      linkOutline,
      linkSharp,
      logOutOutline,
      logOutSharp,
      pricetagsOutline,
      pricetagsSharp,
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
