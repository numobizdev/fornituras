import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IonButton, IonContent, IonSpinner } from '@ionic/angular/standalone';
import { firstValueFrom } from 'rxjs';
import { LandingSectionsComponent } from '../../components/landing-sections/landing-sections.component';
import { LandingSectionPublic } from '../../data/landing.model';
import { LandingService } from '../../data/landing.service';

/**
 * Landing pública pre-login (US3): primera pantalla para visitantes sin sesión. Muestra el contenido
 * institucional configurado (cara PUBLIC) y un botón "Acceder" al inicio de sesión. No monta el shell
 * autenticado ni el menú (eso lo garantiza el arranque, FR-015/FR-016). La respuesta no contiene PII.
 */
@Component({
  selector: 'app-public-landing',
  templateUrl: './public-landing.page.html',
  styleUrls: ['./public-landing.page.scss'],
  imports: [RouterLink, IonContent, IonButton, IonSpinner, LandingSectionsComponent],
})
export class PublicLandingPage implements OnInit {
  private readonly service = inject(LandingService);

  readonly sections = signal<LandingSectionPublic[]>([]);
  readonly isLoading = signal(false);
  readonly hasError = signal(false);

  ngOnInit(): void {
    void this.load();
  }

  async load(): Promise<void> {
    this.isLoading.set(true);
    this.hasError.set(false);
    try {
      this.sections.set(await firstValueFrom(this.service.getPublic()));
    } catch {
      this.hasError.set(true);
    } finally {
      this.isLoading.set(false);
    }
  }
}
