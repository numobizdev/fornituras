import { Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import {
  IonAvatar,
  IonBadge,
  IonButton,
  IonButtons,
  IonContent,
  IonFab,
  IonFabButton,
  IonHeader,
  IonIcon,
  IonItem,
  IonLabel,
  IonList,
  IonMenuButton,
  IonNote,
  IonSearchbar,
  IonSelect,
  IonSelectOption,
  IonSpinner,
  IonTitle,
  IonToolbar,
  ToastController,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import { add, chevronBack, chevronForward, personCircleOutline } from 'ionicons/icons';
import { AuthService } from '../../../../core/services/auth.service';
import { extractApiErrorMessage } from '../../../../core/utils/api-error.util';
import { MunicipiosService } from '../../../municipios/data/municipios.service';
import { MunicipioSummary } from '../../../municipios/data/municipio.model';
import { OfficersService } from '../../data/officers.service';
import { CatalogItem, OfficerSummary } from '../../data/officer.model';

@Component({
  selector: 'app-elementos',
  templateUrl: './elementos.page.html',
  styleUrls: ['./elementos.page.scss'],
  imports: [
    IonHeader,
    IonToolbar,
    IonButtons,
    IonMenuButton,
    IonTitle,
    IonContent,
    IonSearchbar,
    IonSelect,
    IonSelectOption,
    IonList,
    IonItem,
    IonAvatar,
    IonLabel,
    IonNote,
    IonBadge,
    IonButton,
    IonIcon,
    IonSpinner,
    IonFab,
    IonFabButton,
  ],
})
export class ElementosPage implements OnInit {
  private readonly service = inject(OfficersService);
  private readonly municipiosService = inject(MunicipiosService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toastController = inject(ToastController);

  private static readonly PAGE_SIZE = 20;

  readonly items = signal<OfficerSummary[]>([]);
  readonly isLoading = signal(false);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);

  readonly q = signal('');
  readonly municipioId = signal<number | null>(null);
  readonly sexoId = signal<number | null>(null);

  readonly municipios = signal<MunicipioSummary[]>([]);
  readonly sexos = signal<CatalogItem[]>([]);

  readonly canWrite = this.auth.hasRole('ADMIN') || this.auth.hasRole('CAPTURISTA');

  constructor() {
    addIcons({ add, chevronBack, chevronForward, personCircleOutline });
  }

  ngOnInit(): void {
    void this.loadCatalogs();
    void this.load();
  }

  private async loadCatalogs(): Promise<void> {
    try {
      const [municipios, sexos] = await Promise.all([
        firstValueFrom(this.municipiosService.list()),
        firstValueFrom(this.service.listSexos()),
      ]);
      this.municipios.set(municipios);
      this.sexos.set(sexos);
    } catch {
      // Los filtros quedan vacíos; el listado sigue funcionando.
    }
  }

  async load(): Promise<void> {
    this.isLoading.set(true);
    try {
      const result = await firstValueFrom(
        this.service.list({
          q: this.q().trim() || undefined,
          municipioId: this.municipioId() ?? undefined,
          sexoId: this.sexoId() ?? undefined,
          page: this.page(),
          size: ElementosPage.PAGE_SIZE,
        }),
      );
      this.items.set(result.content);
      this.totalPages.set(result.totalPages);
      this.totalElements.set(result.totalElements);
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error), 'danger');
    } finally {
      this.isLoading.set(false);
    }
  }

  applyFilters(): void {
    this.page.set(0);
    void this.load();
  }

  onSearchChange(value: string | null | undefined): void {
    this.q.set(value ?? '');
    this.applyFilters();
  }

  onMunicipioChange(value: number | null): void {
    this.municipioId.set(value);
    this.applyFilters();
  }

  onSexoChange(value: number | null): void {
    this.sexoId.set(value);
    this.applyFilters();
  }

  prev(): void {
    if (this.page() > 0) {
      this.page.update((p) => p - 1);
      void this.load();
    }
  }

  next(): void {
    if (this.page() + 1 < this.totalPages()) {
      this.page.update((p) => p + 1);
      void this.load();
    }
  }

  goToNew(): void {
    void this.router.navigate(['/elementos/nuevo']);
  }

  goToDetail(item: OfficerSummary): void {
    void this.router.navigate(['/elementos', item.id]);
  }

  private async showToast(message: string, color: string): Promise<void> {
    const toast = await this.toastController.create({
      message,
      duration: 3500,
      color,
      position: 'top',
    });
    await toast.present();
  }
}
