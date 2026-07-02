import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
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
  IonList,
  IonListHeader,
  IonNote,
  IonSelect,
  IonSelectOption,
  IonSpinner,
  IonTitle,
  IonToolbar,
  ToastController,
} from '@ionic/angular/standalone';
import { extractApiErrorMessage } from '../../../../core/utils/api-error.util';
import { PhotoPickerComponent } from '../../../../core/media/photo-picker/photo-picker.component';
import { AuthService } from '../../../../core/services/auth.service';
import { UserRole } from '../../../../core/models/auth.model';
import { OfficersService } from '../../data/officers.service';
import { CatalogItem, OfficerCreateRequest } from '../../data/officer.model';

/** Roles que pueden capturar la foto de un elemento (PII); coincide con la matriz del backend (ADR 0013). */
const OFFICER_PHOTO_ROLES: readonly UserRole[] = ['ADMIN', 'SUPERVISOR', 'CAPTURISTA'];

@Component({
  selector: 'app-elemento-form',
  templateUrl: './elemento-form.page.html',
  styleUrls: ['./elemento-form.page.scss'],
  imports: [
    ReactiveFormsModule,
    IonHeader,
    IonToolbar,
    IonButtons,
    IonBackButton,
    IonTitle,
    IonContent,
    IonList,
    IonListHeader,
    IonItem,
    IonLabel,
    IonNote,
    IonInput,
    IonSelect,
    IonSelectOption,
    IonButton,
    IonSpinner,
    PhotoPickerComponent,
  ],
})
export class ElementoFormPage implements OnInit {
  private readonly service = inject(OfficersService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toastController = inject(ToastController);
  private readonly authService = inject(AuthService);

  /** La foto de elemento es PII: solo roles autorizados pueden capturarla (además del gating legal). */
  readonly canCapturePhoto = OFFICER_PHOTO_ROLES.includes(
    this.authService.currentUser()?.role as UserRole,
  );

  readonly officerId = signal<number | null>(null);
  readonly isLoading = signal(false);
  readonly isSubmitting = signal(false);
  readonly piiEnmascarada = signal(false);

  readonly sexos = signal<CatalogItem[]>([]);
  readonly tiposSangre = signal<CatalogItem[]>([]);

  readonly form = this.formBuilder.nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    apellidoPaterno: ['', [Validators.required, Validators.maxLength(120)]],
    apellidoMaterno: ['', [Validators.maxLength(120)]],
    placa: ['', [Validators.required, Validators.maxLength(40)]],
    sexoId: ['', [Validators.required]],
    tipoSangreId: [''],
    municipio: ['', [Validators.maxLength(120)]],
    estado: ['', [Validators.maxLength(120)]],
    curp: ['', [Validators.pattern(/^[A-Za-z0-9]{18}$/)]],
    rfc: ['', [Validators.pattern(/^[A-Za-z0-9]{12,13}$/)]],
    fotoUrl: ['', [Validators.maxLength(500)]],
  });

  get isViewMode(): boolean {
    return this.officerId() !== null;
  }

  /** Mensaje mostrado cuando la captura de foto de elemento está deshabilitada por rol. */
  get photoDisabledReason(): string | null {
    return this.canCapturePhoto
      ? null
      : 'No tienes autorización para capturar la foto del elemento.';
  }

  ngOnInit(): void {
    if (!this.canCapturePhoto) {
      this.form.controls.fotoUrl.disable();
    }
    void this.bootstrap();
  }

  private async bootstrap(): Promise<void> {
    await this.loadCatalogs();
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.officerId.set(Number(idParam));
      await this.loadDetail(Number(idParam));
    }
  }

  private async loadCatalogs(): Promise<void> {
    try {
      const [sexos, tiposSangre] = await Promise.all([
        firstValueFrom(this.service.listSexos()),
        firstValueFrom(this.service.listTiposSangre()),
      ]);
      this.sexos.set(sexos);
      this.tiposSangre.set(tiposSangre);
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error), 'danger');
    }
  }

  private async loadDetail(id: number): Promise<void> {
    this.isLoading.set(true);
    try {
      const detail = await firstValueFrom(this.service.getById(id));
      this.piiEnmascarada.set(detail.piiEnmascarada);
      this.form.patchValue({
        nombre: detail.nombre,
        apellidoPaterno: detail.apellidoPaterno,
        apellidoMaterno: detail.apellidoMaterno ?? '',
        placa: detail.placa,
        sexoId: this.toText(detail.sexoId),
        tipoSangreId: this.toText(detail.tipoSangreId),
        municipio: detail.municipio ?? '',
        estado: detail.estado ?? '',
        curp: detail.curp ?? '',
        rfc: detail.rfc ?? '',
        fotoUrl: detail.fotoUrl ?? '',
      });
      // La ficha es de solo lectura (el backend de 003 implementa alta y consulta).
      this.form.disable();
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error), 'danger');
    } finally {
      this.isLoading.set(false);
    }
  }

  async onSubmit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    const value = this.form.getRawValue();
    const request: OfficerCreateRequest = {
      nombre: value.nombre.trim(),
      apellidoPaterno: value.apellidoPaterno.trim(),
      apellidoMaterno: this.toNullable(value.apellidoMaterno),
      placa: value.placa.trim(),
      sexoId: Number(value.sexoId),
      tipoSangreId: this.toNumber(value.tipoSangreId),
      municipio: this.toNullable(value.municipio),
      estado: this.toNullable(value.estado),
      curp: this.toNullable(value.curp),
      rfc: this.toNullable(value.rfc),
      fotoUrl: this.toNullable(value.fotoUrl),
    };

    try {
      await firstValueFrom(this.service.create(request));
      await this.showToast('Elemento registrado.', 'success');
      await this.router.navigate(['/elementos']);
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error, 'No se pudo guardar el elemento.'), 'danger');
    } finally {
      this.isSubmitting.set(false);
    }
  }

  private toText(value: number | null): string {
    return value === null || value === undefined ? '' : String(value);
  }

  private toNullable(value: string): string | null {
    const trimmed = value.trim();
    return trimmed.length === 0 ? null : trimmed;
  }

  private toNumber(value: string): number | null {
    const trimmed = value.trim();
    if (trimmed.length === 0) {
      return null;
    }
    const parsed = Number(trimmed);
    return Number.isNaN(parsed) ? null : parsed;
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
