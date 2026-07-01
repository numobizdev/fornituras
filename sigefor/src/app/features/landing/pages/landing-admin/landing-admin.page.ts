import { Component, computed, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import {
  AlertController,
  IonBadge,
  IonButton,
  IonButtons,
  IonContent,
  IonFab,
  IonFabButton,
  IonHeader,
  IonIcon,
  IonInput,
  IonItem,
  IonLabel,
  IonList,
  IonMenuButton,
  IonModal,
  IonNote,
  IonSegment,
  IonSegmentButton,
  IonSelect,
  IonSelectOption,
  IonSpinner,
  IonTextarea,
  IonTitle,
  IonToolbar,
  ToastController,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import { add, arrowDownOutline, arrowUpOutline, closeOutline, createOutline, powerOutline, trashOutline } from 'ionicons/icons';
import { extractApiErrorMessage } from '../../../../core/utils/api-error.util';
import {
  LandingScope,
  LandingSectionAdmin,
  LandingSectionRequest,
  LandingSectionType,
  QuickLinkItem,
} from '../../data/landing.model';
import { LandingService } from '../../data/landing.service';

/**
 * Editor de contenido de la landing (US2, solo ADMIN). Permite listar por cara (pública/inicio), crear,
 * editar, activar/desactivar y reordenar secciones. El contenido se captura como texto plano; el escape
 * y la validación de URL los garantizan el render y el backend (ADR 0015).
 */
@Component({
  selector: 'app-landing-admin',
  templateUrl: './landing-admin.page.html',
  styleUrls: ['./landing-admin.page.scss'],
  imports: [
    ReactiveFormsModule,
    IonHeader,
    IonToolbar,
    IonButtons,
    IonMenuButton,
    IonTitle,
    IonContent,
    IonSegment,
    IonSegmentButton,
    IonList,
    IonItem,
    IonLabel,
    IonNote,
    IonBadge,
    IonButton,
    IonIcon,
    IonSpinner,
    IonFab,
    IonFabButton,
    IonModal,
    IonInput,
    IonTextarea,
    IonSelect,
    IonSelectOption,
  ],
})
export class LandingAdminPage {
  private readonly service = inject(LandingService);
  private readonly fb = inject(FormBuilder);
  private readonly toastController = inject(ToastController);
  private readonly alertController = inject(AlertController);

  readonly scope = signal<LandingScope>('HOME');
  readonly sections = signal<LandingSectionAdmin[]>([]);
  readonly isLoading = signal(false);
  readonly isModalOpen = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly isSaving = signal(false);

  readonly modalTitle = computed(() => (this.editingId() === null ? 'Nueva sección' : 'Editar sección'));

  readonly sectionTypes: LandingSectionType[] = ['HERO', 'ANNOUNCEMENT', 'QUICK_LINKS', 'RICH_TEXT'];

  readonly form = this.fb.group({
    type: this.fb.nonNullable.control<LandingSectionType>('HERO'),
    titulo: this.fb.nonNullable.control(''),
    subtitulo: this.fb.nonNullable.control(''),
    cuerpo: this.fb.nonNullable.control(''),
    imagenUrl: this.fb.nonNullable.control(''),
    ctaLabel: this.fb.nonNullable.control(''),
    ctaUrl: this.fb.nonNullable.control(''),
    orden: this.fb.nonNullable.control(0),
    quickLinks: this.fb.array<FormGroup>([]),
  });

  constructor() {
    addIcons({ add, createOutline, powerOutline, arrowUpOutline, arrowDownOutline, closeOutline, trashOutline });
  }

  // Recarga en cada entrada (Ionic no re-ejecuta ngOnInit al volver a la página).
  ionViewWillEnter(): void {
    void this.load();
  }

  get quickLinks(): FormArray<FormGroup> {
    return this.form.controls.quickLinks;
  }

  get isQuickLinks(): boolean {
    return this.form.controls.type.value === 'QUICK_LINKS';
  }

  async load(): Promise<void> {
    this.isLoading.set(true);
    try {
      this.sections.set(await firstValueFrom(this.service.listSections(this.scope())));
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error), 'danger');
    } finally {
      this.isLoading.set(false);
    }
  }

  onScopeChange(value: string | undefined): void {
    if (value === 'PUBLIC' || value === 'HOME') {
      this.scope.set(value);
      void this.load();
    }
  }

  openNew(): void {
    this.editingId.set(null);
    this.resetForm();
    const nextOrden = this.sections().length;
    this.form.controls.orden.setValue(nextOrden);
    this.isModalOpen.set(true);
  }

  openEdit(section: LandingSectionAdmin): void {
    this.editingId.set(section.id);
    this.resetForm();
    this.form.patchValue({
      type: section.type,
      titulo: section.titulo ?? '',
      subtitulo: section.subtitulo ?? '',
      cuerpo: section.cuerpo ?? '',
      imagenUrl: section.imagenUrl ?? '',
      ctaLabel: section.ctaLabel ?? '',
      ctaUrl: section.ctaUrl ?? '',
      orden: section.orden,
    });
    for (const link of section.quickLinks ?? []) {
      this.quickLinks.push(this.newQuickLinkGroup(link));
    }
    this.isModalOpen.set(true);
  }

  closeModal(): void {
    this.isModalOpen.set(false);
  }

  onTypeChange(): void {
    if (this.isQuickLinks && this.quickLinks.length === 0) {
      this.addQuickLink();
    }
  }

  addQuickLink(): void {
    this.quickLinks.push(this.newQuickLinkGroup());
  }

  removeQuickLink(index: number): void {
    this.quickLinks.removeAt(index);
  }

  async submit(): Promise<void> {
    this.isSaving.set(true);
    const value = this.form.getRawValue();
    const request: LandingSectionRequest = {
      scope: this.scope(),
      type: value.type,
      titulo: this.nullable(value.titulo),
      subtitulo: this.nullable(value.subtitulo),
      cuerpo: this.nullable(value.cuerpo),
      imagenUrl: this.nullable(value.imagenUrl),
      ctaLabel: this.nullable(value.ctaLabel),
      ctaUrl: this.nullable(value.ctaUrl),
      orden: Number(value.orden) || 0,
      quickLinks: value.type === 'QUICK_LINKS' ? this.collectQuickLinks() : null,
    };

    try {
      const id = this.editingId();
      if (id === null) {
        await firstValueFrom(this.service.createSection(request));
        await this.showToast('Sección creada.', 'success');
      } else {
        await firstValueFrom(this.service.updateSection(id, request));
        await this.showToast('Sección actualizada.', 'success');
      }
      this.isModalOpen.set(false);
      await this.load();
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error, 'No se pudo guardar la sección.'), 'danger');
    } finally {
      this.isSaving.set(false);
    }
  }

  async toggleActive(section: LandingSectionAdmin): Promise<void> {
    if (!section.active) {
      await this.activate(section);
      return;
    }
    const alert = await this.alertController.create({
      header: 'Desactivar sección',
      message: `¿Desactivar "${section.titulo ?? section.type}"? Dejará de mostrarse, pero no se borra.`,
      buttons: [
        { text: 'Cancelar', role: 'cancel' },
        {
          text: 'Desactivar',
          role: 'destructive',
          handler: () => {
            void this.deactivate(section);
          },
        },
      ],
    });
    await alert.present();
  }

  async moveUp(index: number): Promise<void> {
    if (index <= 0) {
      return;
    }
    await this.swap(index, index - 1);
  }

  async moveDown(index: number): Promise<void> {
    if (index >= this.sections().length - 1) {
      return;
    }
    await this.swap(index, index + 1);
  }

  private async swap(a: number, b: number): Promise<void> {
    const list = this.sections();
    const first = list[a];
    const second = list[b];
    try {
      await firstValueFrom(
        this.service.reorder({
          items: [
            { id: first.id, orden: second.orden },
            { id: second.id, orden: first.orden },
          ],
        }),
      );
      await this.load();
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error), 'danger');
    }
  }

  private async deactivate(section: LandingSectionAdmin): Promise<void> {
    try {
      await firstValueFrom(this.service.deactivateSection(section.id));
      await this.showToast('Sección desactivada.', 'success');
      await this.load();
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error), 'danger');
    }
  }

  private async activate(section: LandingSectionAdmin): Promise<void> {
    try {
      await firstValueFrom(this.service.activateSection(section.id));
      await this.showToast('Sección activada.', 'success');
      await this.load();
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error), 'danger');
    }
  }

  private collectQuickLinks(): QuickLinkItem[] {
    return this.quickLinks.controls.map((group) => ({
      label: (group.get('label')?.value ?? '').trim(),
      url: (group.get('url')?.value ?? '').trim(),
      icon: this.nullable(group.get('icon')?.value ?? ''),
    }));
  }

  private newQuickLinkGroup(link?: QuickLinkItem): FormGroup {
    return this.fb.group({
      label: this.fb.nonNullable.control(link?.label ?? ''),
      url: this.fb.nonNullable.control(link?.url ?? ''),
      icon: this.fb.nonNullable.control(link?.icon ?? ''),
    });
  }

  private resetForm(): void {
    this.form.reset({
      type: 'HERO',
      titulo: '',
      subtitulo: '',
      cuerpo: '',
      imagenUrl: '',
      ctaLabel: '',
      ctaUrl: '',
      orden: 0,
    });
    this.quickLinks.clear();
  }

  private nullable(value: string | null | undefined): string | null {
    const trimmed = (value ?? '').trim();
    return trimmed.length === 0 ? null : trimmed;
  }

  private async showToast(message: string, color: string): Promise<void> {
    const toast = await this.toastController.create({ message, duration: 3500, color, position: 'top' });
    await toast.present();
  }
}
