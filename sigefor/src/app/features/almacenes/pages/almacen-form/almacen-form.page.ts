import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import {
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonInput,
  IonItem,
  IonLabel,
  IonList,
  IonListHeader,
  IonModal,
  IonNote,
  IonSearchbar,
  IonSelect,
  IonSelectOption,
  IonSpinner,
  IonTextarea,
  IonTitle,
  IonToolbar,
  ToastController,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import { closeOutline, personOutline, searchOutline } from 'ionicons/icons';
import { CATALOG_CODES, CatalogItemSummary } from '../../../../core/catalog/catalog.model';
import { CatalogService } from '../../../../core/catalog/catalog.service';
import { FieldErrorsComponent } from '../../../../core/forms/field-errors.component';
import { extractApiErrorMessage } from '../../../../core/utils/api-error.util';
import { UserSummary } from '../../../usuarios/data/user.model';
import { UsersService } from '../../../usuarios/data/users.service';
import { WarehousesService } from '../../data/warehouses.service';
import { WarehouseCreateRequest } from '../../data/warehouse.model';

@Component({
  selector: 'app-almacen-form',
  templateUrl: './almacen-form.page.html',
  styleUrls: ['./almacen-form.page.scss'],
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
    IonTextarea,
    IonSelect,
    IonSelectOption,
    IonButton,
    IonIcon,
    IonSpinner,
    IonModal,
    IonSearchbar,
    FieldErrorsComponent,
  ],
})
export class AlmacenFormPage implements OnInit {
  private readonly service = inject(WarehousesService);
  private readonly catalog = inject(CatalogService);
  private readonly users = inject(UsersService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toastController = inject(ToastController);

  readonly warehouseId = signal<number | null>(null);
  readonly isLoading = signal(false);
  readonly isSubmitting = signal(false);
  readonly tipoOptions = signal<CatalogItemSummary[]>([]);

  // Responsable: se elige de un buscador de usuarios (no se teclea el id).
  readonly userOptions = signal<UserSummary[]>([]);
  readonly responsableId = signal<number | null>(null);
  readonly responsableNombre = signal<string | null>(null);
  readonly isUserModalOpen = signal(false);
  readonly userSearch = signal('');

  readonly filteredUsers = computed(() => {
    const term = this.userSearch().trim().toLowerCase();
    const list = this.userOptions();
    if (term.length === 0) {
      return list;
    }
    return list.filter(
      (u) => u.name.toLowerCase().includes(term) || u.email.toLowerCase().includes(term),
    );
  });

  readonly form = this.formBuilder.nonNullable.group({
    codigo: ['', [Validators.required, Validators.maxLength(40)]],
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    tipoItemId: ['', [Validators.required]],
    municipio: ['', [Validators.maxLength(120)]],
    estado: ['', [Validators.maxLength(120)]],
    direccion: ['', [Validators.maxLength(255)]],
    cp: ['', [Validators.maxLength(10)]],
    latitud: [''],
    longitud: [''],
    telefono: ['', [Validators.maxLength(30)]],
    emailContacto: ['', [Validators.email, Validators.maxLength(255)]],
    capacidad: [''],
    observaciones: ['', [Validators.maxLength(500)]],
  });

  constructor() {
    addIcons({ searchOutline, personOutline, closeOutline });
  }

  get isEditing(): boolean {
    return this.warehouseId() !== null;
  }

  ngOnInit(): void {
    void this.loadTipos();
    void this.loadUsers();
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.warehouseId.set(Number(idParam));
      void this.loadDetail(Number(idParam));
    }
  }

  private async loadTipos(): Promise<void> {
    try {
      this.tipoOptions.set(
        await firstValueFrom(this.catalog.listActiveItems(CATALOG_CODES.TIPO_ALMACEN)),
      );
    } catch {
      this.tipoOptions.set([]);
    }
  }

  private async loadUsers(): Promise<void> {
    try {
      const page = await firstValueFrom(this.users.list({ size: 200 }));
      this.userOptions.set(page.content);
      this.resolveResponsableName();
    } catch {
      // El selector de responsable queda vacío; el campo es opcional.
      this.userOptions.set([]);
    }
  }

  private async loadDetail(id: number): Promise<void> {
    this.isLoading.set(true);
    try {
      const detail = await firstValueFrom(this.service.getById(id));
      this.form.patchValue({
        codigo: detail.codigo,
        nombre: detail.nombre,
        tipoItemId: this.toText(detail.tipoItemId),
        municipio: detail.municipio ?? '',
        estado: detail.estado ?? '',
        direccion: detail.direccion ?? '',
        cp: detail.cp ?? '',
        latitud: this.toText(detail.latitud),
        longitud: this.toText(detail.longitud),
        telefono: detail.telefono ?? '',
        emailContacto: detail.emailContacto ?? '',
        capacidad: this.toText(detail.capacidad),
        observaciones: detail.observaciones ?? '',
      });
      this.responsableId.set(detail.responsableId ?? null);
      this.resolveResponsableName();
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error), 'danger');
    } finally {
      this.isLoading.set(false);
    }
  }

  openUserModal(): void {
    this.userSearch.set('');
    this.isUserModalOpen.set(true);
  }

  closeUserModal(): void {
    this.isUserModalOpen.set(false);
  }

  onUserSearch(value: string | null | undefined): void {
    this.userSearch.set(value ?? '');
  }

  selectUser(user: UserSummary): void {
    this.responsableId.set(user.id);
    this.responsableNombre.set(user.name);
    this.isUserModalOpen.set(false);
  }

  clearResponsable(): void {
    this.responsableId.set(null);
    this.responsableNombre.set(null);
  }

  async onSubmit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    const value = this.form.getRawValue();
    const request: WarehouseCreateRequest = {
      codigo: value.codigo.trim(),
      nombre: value.nombre.trim(),
      tipoItemId: Number(value.tipoItemId),
      municipio: this.toNullable(value.municipio),
      estado: this.toNullable(value.estado),
      direccion: this.toNullable(value.direccion),
      cp: this.toNullable(value.cp),
      latitud: this.toNumber(value.latitud),
      longitud: this.toNumber(value.longitud),
      responsableId: this.responsableId(),
      telefono: this.toNullable(value.telefono),
      emailContacto: this.toNullable(value.emailContacto),
      capacidad: this.toNumber(value.capacidad),
      observaciones: this.toNullable(value.observaciones),
    };

    try {
      const id = this.warehouseId();
      if (id !== null) {
        await firstValueFrom(this.service.update(id, request));
        await this.showToast('Almacén actualizado.', 'success');
      } else {
        await firstValueFrom(this.service.create(request));
        await this.showToast('Almacén creado.', 'success');
      }
      await this.router.navigate(['/almacenes']);
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error, 'No se pudo guardar el almacén.'), 'danger');
    } finally {
      this.isSubmitting.set(false);
    }
  }

  private resolveResponsableName(): void {
    const id = this.responsableId();
    if (id === null) {
      this.responsableNombre.set(null);
      return;
    }
    const match = this.userOptions().find((u) => u.id === id);
    if (match) {
      this.responsableNombre.set(match.name);
    }
  }

  private toText(value: number | null | undefined): string {
    return value === null || value === undefined ? '' : String(value);
  }

  private toNullable(value: unknown): string | null {
    const trimmed = this.asString(value).trim();
    return trimmed.length === 0 ? null : trimmed;
  }

  private toNumber(value: unknown): number | null {
    const trimmed = this.asString(value).trim();
    if (trimmed.length === 0) {
      return null;
    }
    const parsed = Number(trimmed);
    return Number.isNaN(parsed) ? null : parsed;
  }

  /** Los `ion-input type="number"` entregan `number` (no string) al control; normalizamos con seguridad. */
  private asString(value: unknown): string {
    if (value === null || value === undefined) {
      return '';
    }
    return typeof value === 'string' ? value : String(value);
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
