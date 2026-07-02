import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import {
  IonBackButton,
  IonButton,
  IonButtons,
  IonCard,
  IonCardContent,
  IonCardHeader,
  IonCardTitle,
  IonContent,
  IonHeader,
  IonItem,
  IonLabel,
  IonList,
  IonSpinner,
  IonTitle,
  IonToolbar,
  ToastController,
} from '@ionic/angular/standalone';
import { extractApiErrorMessage } from '../../../../core/utils/api-error.util';
import { triggerFileDownload } from '../../data/download.util';
import {
  formatCodigoRange,
  LABEL_POSITION_LABELS,
  LoteQrSummary,
  QrExportFormat,
} from '../../data/qr-lote.model';
import { QrLotesService } from '../../data/qr-lotes.service';

@Component({
  selector: 'app-qr-lote-exito',
  templateUrl: './qr-lote-exito.page.html',
  styleUrls: ['./qr-lote-exito.page.scss'],
  imports: [
    DatePipe,
    RouterLink,
    IonHeader,
    IonToolbar,
    IonButtons,
    IonBackButton,
    IonTitle,
    IonContent,
    IonCard,
    IonCardHeader,
    IonCardTitle,
    IonCardContent,
    IonList,
    IonItem,
    IonLabel,
    IonButton,
    IonSpinner,
  ],
})
export class QrLoteExitoPage implements OnInit {
  private readonly service = inject(QrLotesService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toastController = inject(ToastController);

  readonly lote = signal<LoteQrSummary | null>(null);
  readonly isLoading = signal(true);
  readonly isDownloading = signal(false);
  readonly downloadMessage = signal('');

  readonly formatRange = formatCodigoRange;
  readonly labelPositionLabels = LABEL_POSITION_LABELS;

  private selectedFormat: QrExportFormat = 'PDF';
  private autoDownloadDone = false;

  ngOnInit(): void {
    const format = this.route.snapshot.queryParamMap.get('format');
    if (format === 'ZIP' || format === 'PDF') {
      this.selectedFormat = format;
    }
    void this.load();
  }

  private async load(): Promise<void> {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id)) {
      void this.router.navigate(['/qr-lotes']);
      return;
    }

    this.isLoading.set(true);
    try {
      const lote = await firstValueFrom(this.service.getLote(id));
      this.lote.set(lote);
      if (!this.autoDownloadDone) {
        this.autoDownloadDone = true;
        await this.downloadSelectedFormat();
      }
    } catch (error) {
      await this.showError(extractApiErrorMessage(error, 'No se encontró el lote.'));
      void this.router.navigate(['/qr-lotes']);
    } finally {
      this.isLoading.set(false);
    }
  }

  async downloadPdf(): Promise<void> {
    await this.download('PDF');
  }

  async downloadZip(): Promise<void> {
    await this.download('ZIP');
  }

  private async downloadSelectedFormat(): Promise<void> {
    await this.download(this.selectedFormat);
  }

  private async download(format: QrExportFormat): Promise<void> {
    const lote = this.lote();
    if (!lote) return;

    this.isDownloading.set(true);
    this.downloadMessage.set(
      format === 'ZIP'
        ? 'Generando ZIP… Esto puede tardar en lotes grandes.'
        : 'Generando PDF… Esto puede tardar en lotes grandes.',
    );

    try {
      const blob =
        format === 'ZIP'
          ? await firstValueFrom(this.service.downloadZipOriginal(lote.id))
          : await firstValueFrom(this.service.downloadPdfOriginal(lote.id));
      triggerFileDownload(blob, `lote-qr-${lote.id}.${format === 'ZIP' ? 'zip' : 'pdf'}`);
    } catch (error) {
      await this.showError(extractApiErrorMessage(error, 'No se pudo generar el archivo.'));
    } finally {
      this.isDownloading.set(false);
      this.downloadMessage.set('');
    }
  }

  private async showError(message: string): Promise<void> {
    const toast = await this.toastController.create({
      message,
      duration: 5000,
      color: 'danger',
      position: 'top',
    });
    await toast.present();
  }
}
