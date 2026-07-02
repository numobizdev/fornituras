import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
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
  IonInput,
  IonItem,
  IonLabel,
  IonList,
  IonNote,
  IonSelect,
  IonSelectOption,
  IonSpinner,
  IonTitle,
  IonToolbar,
  ToastController,
} from '@ionic/angular/standalone';
import { extractApiErrorMessage } from '../../../../core/utils/api-error.util';
import { triggerFileDownload } from '../../data/download.util';
import {
  formatCodigoRange,
  LABEL_POSITION_OPTIONS,
  LabelPosition,
  LoteQrSummary,
  reprintFromLote,
} from '../../data/qr-lote.model';
import { QrLotesService } from '../../data/qr-lotes.service';

@Component({
  selector: 'app-qr-lote-detail',
  templateUrl: './qr-lote-detail.page.html',
  styleUrls: ['./qr-lote-detail.page.scss'],
  imports: [
    DatePipe,
    ReactiveFormsModule,
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
    IonInput,
    IonSelect,
    IonSelectOption,
    IonButton,
    IonSpinner,
  ],
})
export class QrLoteDetailPage implements OnInit {
  private readonly service = inject(QrLotesService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toastController = inject(ToastController);
  private readonly formBuilder = inject(FormBuilder);

  readonly lote = signal<LoteQrSummary | null>(null);
  readonly isLoading = signal(true);
  readonly isExporting = signal(false);
  readonly exportMessage = signal('');

  readonly labelOptions = LABEL_POSITION_OPTIONS;
  readonly formatRange = formatCodigoRange;

  readonly form = this.formBuilder.nonNullable.group({
    qrSizeCm: [3, [Validators.required, Validators.min(1), Validators.max(15)]],
    paddingCm: [0.5, [Validators.required, Validators.min(0), Validators.max(5)]],
    labelPosition: ['BOTTOM' as LabelPosition, [Validators.required]],
    mostrarBordes: [true, [Validators.required]],
  });

  ngOnInit(): void {
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
      this.form.patchValue(reprintFromLote(lote));
    } catch (error) {
      await this.showError(extractApiErrorMessage(error, 'No se encontró el lote.'));
      void this.router.navigate(['/qr-lotes']);
    } finally {
      this.isLoading.set(false);
    }
  }

  async downloadOriginalPdf(): Promise<void> {
    const lote = this.lote();
    if (!lote) return;
    await this.runExport(
      'Generando PDF… Esto puede tardar en lotes grandes.',
      () => this.service.downloadPdfOriginal(lote.id),
      `lote-qr-${lote.id}.pdf`,
    );
  }

  async downloadOriginalZip(): Promise<void> {
    const lote = this.lote();
    if (!lote) return;
    await this.runExport(
      'Generando ZIP… Esto puede tardar en lotes grandes.',
      () => this.service.downloadZipOriginal(lote.id),
      `lote-qr-${lote.id}.zip`,
    );
  }

  async reprintPdf(): Promise<void> {
    const lote = this.lote();
    if (!lote || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    await this.runExport(
      'Generando PDF… Esto puede tardar en lotes grandes.',
      () => this.service.downloadPdfReprint(lote.id, this.form.getRawValue()),
      `lote-qr-${lote.id}-reprint.pdf`,
    );
  }

  async reprintZip(): Promise<void> {
    const lote = this.lote();
    if (!lote || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    await this.runExport(
      'Generando ZIP… Esto puede tardar en lotes grandes.',
      () => this.service.downloadZipReprint(lote.id, this.form.getRawValue()),
      `lote-qr-${lote.id}-reprint.zip`,
    );
  }

  private async runExport(
    message: string,
    request: () => import('rxjs').Observable<Blob>,
    filename: string,
  ): Promise<void> {
    this.isExporting.set(true);
    this.exportMessage.set(message);
    try {
      const blob = await firstValueFrom(request());
      triggerFileDownload(blob, filename);
    } catch (error) {
      await this.showError(extractApiErrorMessage(error, 'No se pudo generar el archivo.'));
    } finally {
      this.isExporting.set(false);
      this.exportMessage.set('');
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
