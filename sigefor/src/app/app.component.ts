
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

  public readonly appPages: NavItem[] = APP_NAV_ITEMS;
  public readonly currentUser = this.authService.currentUser;

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
    });
  }

  async logout(): Promise<void> {
    await this.authService.logout();
  }
}
