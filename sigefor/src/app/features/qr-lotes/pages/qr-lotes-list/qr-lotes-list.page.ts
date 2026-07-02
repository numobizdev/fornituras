import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import {
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonItem,
  IonLabel,
  IonList,
  IonMenuButton,
  IonSpinner,
  IonTitle,
  IonToolbar,
  ToastController,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import { add, chevronForwardOutline } from 'ionicons/icons';
import { extractApiErrorMessage } from '../../../../core/utils/api-error.util';
import {
  formatCodigoRange,
  LABEL_POSITION_LABELS,
  LoteQrSummary,
} from '../../data/qr-lote.model';
import { QrLotesService } from '../../data/qr-lotes.service';

@Component({
  selector: 'app-qr-lotes-list',
  templateUrl: './qr-lotes-list.page.html',
  styleUrls: ['./qr-lotes-list.page.scss'],
  imports: [
    DatePipe,
    IonHeader,
    IonToolbar,
    IonButtons,
    IonMenuButton,
    IonTitle,
    IonContent,
    IonButton,
    IonIcon,
    IonSpinner,
    IonList,
    IonItem,
    IonLabel,
  ],
})
export class QrLotesListPage implements OnInit {
  private readonly service = inject(QrLotesService);
  private readonly toastController = inject(ToastController);
  private readonly router = inject(Router);

  readonly lotes = signal<LoteQrSummary[]>([]);
  readonly isLoading = signal(false);

  readonly formatRange = formatCodigoRange;
  readonly labelPositionLabels = LABEL_POSITION_LABELS;

  constructor() {
    addIcons({ add, chevronForwardOutline });
  }

  ngOnInit(): void {
    void this.load();
  }

  async load(): Promise<void> {
    this.isLoading.set(true);
    try {
      const lotes = await firstValueFrom(this.service.listLotes());
      this.lotes.set(lotes);
    } catch (error) {
      await this.showError(extractApiErrorMessage(error, 'No se pudieron cargar los lotes.'));
    } finally {
      this.isLoading.set(false);
    }
  }

  goToGenerate(): void {
    void this.router.navigate(['/qr-lotes/generar']);
  }

  goToDetail(id: number): void {
    void this.router.navigate(['/qr-lotes', id]);
  }

  private async showError(message: string): Promise<void> {
    const toast = await this.toastController.create({
      message,
      duration: 4000,
      color: 'danger',
      position: 'top',
    });
    await toast.present();
  }
}
