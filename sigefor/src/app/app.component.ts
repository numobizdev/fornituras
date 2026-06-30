
import { Component } from '@angular/core';
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
  peopleOutline,
  peopleSharp,
} from 'ionicons/icons';
import { APP_NAV_ITEMS } from './core/constants/app-navigation';
import { NavItem } from './core/models/nav-item.model';

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
    IonRouterLink,
    IonRouterOutlet,
  ],
})
export class AppComponent {
  public readonly appPages: NavItem[] = APP_NAV_ITEMS;

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
    });
  }
}
