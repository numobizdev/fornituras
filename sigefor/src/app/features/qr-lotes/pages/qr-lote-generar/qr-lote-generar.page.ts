import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import {
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonInput,
  IonItem,
  IonLabel,
  IonNote,
  IonSelect,
  IonSelectOption,
  IonSpinner,
  IonText,
  IonTitle,
  IonToolbar,
  ToastController,
} from '@ionic/angular/standalone';
import { extractApiErrorMessage } from '../../../../core/utils/api-error.util';
import {
  defaultGenerateQrRequest,
  LABEL_POSITION_OPTIONS,
  QR_MAX_BATCH_SIZE,
  QrExportFormat,
} from '../../data/qr-lote.model';
import { QrLotesService } from '../../data/qr-lotes.service';

@Component({
  selector: 'app-qr-lote-generar',
  templateUrl: './qr-lote-generar.page.html',
  styleUrls: ['./qr-lote-generar.page.scss'],
  imports: [
    ReactiveFormsModule,
    IonHeader,
    IonToolbar,
    IonButtons,
    IonBackButton,
    IonTitle,
    IonContent,
    IonItem,
    IonInput,
    IonSelect,
    IonSelectOption,
    IonNote,
    IonButton,
    IonSpinner,
    IonText,
  ],
})
export class QrLoteGenerarPage {
  private readonly service = inject(QrLotesService);
  private readonly router = inject(Router);
  private readonly toastController = inject(ToastController);
  private readonly formBuilder = inject(FormBuilder);

  readonly isSubmitting = signal(false);
  readonly maxBatchSize = QR_MAX_BATCH_SIZE;
  readonly labelOptions = LABEL_POSITION_OPTIONS;
  readonly exportFormats: ReadonlyArray<{ value: QrExportFormat; label: string }> = [
    { value: 'PDF', label: 'PDF imprimible' },
    { value: 'ZIP', label: 'ZIP (PNG por código)' },
  ];

  readonly form = this.formBuilder.nonNullable.group({
    descripcion: ['', [Validators.required, Validators.maxLength(255)]],
    cantidad: [
      defaultGenerateQrRequest().cantidad,
      [Validators.required, Validators.min(1), Validators.max(QR_MAX_BATCH_SIZE)],
    ],
    qrSizeCm: [
      defaultGenerateQrRequest().qrSizeCm,
      [Validators.required, Validators.min(1), Validators.max(15)],
    ],
    paddingCm: [
      defaultGenerateQrRequest().paddingCm,
      [Validators.required, Validators.min(0), Validators.max(5)],
    ],
    labelPosition: [defaultGenerateQrRequest().labelPosition, [Validators.required]],
    mostrarBordes: [defaultGenerateQrRequest().mostrarBordes, [Validators.required]],
    exportFormat: ['PDF' as QrExportFormat, [Validators.required]],
  });

  get submitLabel(): string {
    return this.form.controls.exportFormat.value === 'ZIP'
      ? 'Generar lote y descargar ZIP (PNG)'
      : 'Generar lote y descargar PDF';
  }

  get loadingMessage(): string {
    return this.form.controls.exportFormat.value === 'ZIP'
      ? 'Generando códigos QR y ZIP… Esto puede tardar unos minutos en lotes grandes.'
      : 'Generando códigos QR y PDF… Esto puede tardar unos minutos en lotes grandes.';
  }

  async onSubmit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    const { exportFormat, ...payload } = this.form.getRawValue();

    try {
      const lote = await firstValueFrom(this.service.generateLote(payload));
      await this.router.navigate(['/qr-lotes', lote.id, 'exito'], {
        queryParams: { format: exportFormat },
      });
    } catch (error) {
      await this.showError(extractApiErrorMessage(error, 'No se pudo generar el lote.'));
    } finally {
      this.isSubmitting.set(false);
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
