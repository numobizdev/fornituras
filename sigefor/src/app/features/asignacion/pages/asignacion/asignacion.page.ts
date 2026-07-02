import { Component, computed, inject, signal, viewChild } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import {
  AlertController,
  IonBadge,
  IonButton,
  IonButtons,
  IonCard,
  IonCardContent,
  IonCardHeader,
  IonCardSubtitle,
  IonCardTitle,
  IonContent,
  IonHeader,
  IonIcon,
  IonItem,
  IonLabel,
  IonList,
  IonListHeader,
  IonMenuButton,
  IonNote,
  IonSearchbar,
  IonSpinner,
  IonTitle,
  IonToolbar,
  ToastController,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import { checkmarkCircle, closeCircle } from 'ionicons/icons';
import { ROLE_POLICY } from '../../../../core/security/role-policy';
import { AuthService } from '../../../../core/services/auth.service';
import { extractApiErrorMessage } from '../../../../core/utils/api-error.util';
import { QrScanComponent } from '../../../../core/qr-scan/qr-scan.component';
import { QrCaptureError } from '../../../../core/qr-scan/qr-scan.types';
import { EquipmentService } from '../../../fornituras/data/equipment.service';
import { EquipmentDetail } from '../../../fornituras/data/equipment.model';
import { OfficersService } from '../../../elementos/data/officers.service';
import { OfficerSummary } from '../../../elementos/data/officer.model';
import { AssignmentsService } from '../../data/assignments.service';
import { AssignmentSummary } from '../../data/assignment.model';

type EquipmentFeedbackColor = 'success' | 'warning' | 'danger' | 'medium';

interface EquipmentFeedback {
  title: string;
  message: string;
  color: EquipmentFeedbackColor;
}

@Component({
  selector: 'app-asignacion',
  templateUrl: './asignacion.page.html',
  styleUrls: ['./asignacion.page.scss'],
  imports: [
    IonHeader,
    IonToolbar,
    IonButtons,
    IonMenuButton,
    IonTitle,
    IonContent,
    IonCard,
    IonCardHeader,
    IonCardTitle,
    IonCardSubtitle,
    IonCardContent,
    IonList,
    IonListHeader,
    IonItem,
    IonLabel,
    IonNote,
    IonSearchbar,
    IonBadge,
    IonButton,
    IonIcon,
    IonSpinner,
    QrScanComponent,
  ],
})
export class AsignacionPage {
  private readonly qrScan = viewChild(QrScanComponent);

  private readonly assignmentsService = inject(AssignmentsService);
  private readonly equipmentService = inject(EquipmentService);
  private readonly officersService = inject(OfficersService);
  private readonly auth = inject(AuthService);
  private readonly toastController = inject(ToastController);
  private readonly alertController = inject(AlertController);

  private static readonly PAGE_SIZE = 20;

  readonly vigentes = signal<AssignmentSummary[]>([]);
  readonly isLoading = signal(false);
  readonly page = signal(0);
  readonly totalPages = signal(0);

  // Paso 1: fornitura
  readonly codigo = signal('');
  readonly resolvingEquipment = signal(false);
  readonly equipment = signal<EquipmentDetail | null>(null);
  readonly equipmentFeedback = signal<EquipmentFeedback | null>(null);

  // Paso 2: elemento
  readonly officerQuery = signal('');
  readonly searchingOfficer = signal(false);
  readonly officerResults = signal<OfficerSummary[]>([]);
  readonly selectedOfficer = signal<OfficerSummary | null>(null);

  readonly isSubmitting = signal(false);
  readonly canAssign = computed(() => this.auth.hasAnyRole(ROLE_POLICY.WRITE_OPERATIONS));

  /** Búsqueda hecha (≥2 chars) sin coincidencias y sin selección previa: se comunica, no silencio. */
  readonly showNoResults = computed(
    () =>
      this.officerQuery().trim().length >= 2 &&
      this.officerResults().length === 0 &&
      this.selectedOfficer() === null,
  );

  constructor() {
    addIcons({ checkmarkCircle, closeCircle });
  }

  // Recarga en cada entrada (Ionic no re-ejecuta ngOnInit al volver de asignar/devolver).
  ionViewWillEnter(): void {
    void this.loadVigentes();
  }

  async loadVigentes(): Promise<void> {
    this.isLoading.set(true);
    try {
      const result = await firstValueFrom(
        this.assignmentsService.listVigentes({ page: this.page(), size: AsignacionPage.PAGE_SIZE }),
      );
      this.vigentes.set(result.content);
      this.totalPages.set(result.totalPages);
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error), 'danger');
    } finally {
      this.isLoading.set(false);
    }
  }

  get equipmentAvailable(): boolean {
    return this.equipment()?.status === 'DISPONIBLE';
  }

  get canConfirm(): boolean {
    return this.equipmentAvailable && this.selectedOfficer() !== null;
  }

  /** Código capturado por lector/cámara/manual (componente 014): resuelve la fornitura server-side. */
  async onCodeCaptured(code: string): Promise<void> {
    this.codigo.set(code);
    await this.resolveEquipment(code);
  }

  async onCaptureError(error: QrCaptureError): Promise<void> {
    this.setEquipmentFeedback('No se pudo usar la cámara', error.message, 'warning');
  }

  dismissEquipmentFeedback(): void {
    this.equipmentFeedback.set(null);
  }

  async resolveEquipment(code?: string): Promise<void> {
    const resolvedCode = (code ?? this.codigo()).trim();
    if (!resolvedCode) {
      return;
    }
    this.codigo.set(resolvedCode);
    this.resolvingEquipment.set(true);
    this.equipment.set(null);
    this.equipmentFeedback.set(null);
    try {
      const equipment = await firstValueFrom(this.equipmentService.getByCodigo(resolvedCode));
      this.equipment.set(equipment);
      if (equipment.status === 'DISPONIBLE') {
        this.setEquipmentFeedback(
          'Fornitura encontrada',
          `Código ${equipment.codigoQr} identificado y disponible para asignar.`,
          'success',
        );
        return;
      }
      const message =
        equipment.status === 'ASIGNADA'
          ? `La fornitura ${equipment.codigoQr} ya está asignada a un elemento y no puede asignarse de nuevo.`
          : `La fornitura ${equipment.codigoQr} existe, pero no está disponible en almacén.`;
      this.setEquipmentFeedback('Fornitura no disponible', message, 'warning');
    } catch (error) {
      const message = this.equipmentLookupErrorMessage(error, resolvedCode);
      const title =
        error instanceof HttpErrorResponse && error.status === 404
          ? 'Fornitura no encontrada'
          : 'Error al buscar fornitura';
      this.setEquipmentFeedback(title, message, 'danger');
    } finally {
      this.resolvingEquipment.set(false);
    }
  }

  async searchOfficer(value: string | null | undefined): Promise<void> {
    const q = (value ?? '').trim();
    this.officerQuery.set(q);
    if (q.length < 2) {
      this.officerResults.set([]);
      return;
    }
    this.searchingOfficer.set(true);
    try {
      const result = await firstValueFrom(this.officersService.list({ q, size: 10 }));
      this.officerResults.set(result.content);
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error), 'danger');
    } finally {
      this.searchingOfficer.set(false);
    }
  }

  selectOfficer(officer: OfficerSummary): void {
    this.selectedOfficer.set(officer);
    this.officerResults.set([]);
    this.officerQuery.set(officer.nombreCompleto);
  }

  async confirmAssign(): Promise<void> {
    const equipment = this.equipment();
    const officer = this.selectedOfficer();
    if (!equipment || !officer || !this.equipmentAvailable) {
      return;
    }
    this.isSubmitting.set(true);
    try {
      await firstValueFrom(
        this.assignmentsService.assign({ equipmentId: equipment.id, officerId: officer.id }),
      );
      await this.showToast('Fornitura asignada.', 'success');
      this.clear();
      this.page.set(0);
      await this.loadVigentes();
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error, 'No se pudo asignar.'), 'danger');
    } finally {
      this.isSubmitting.set(false);
    }
  }

  clear(): void {
    this.codigo.set('');
    this.equipment.set(null);
    this.equipmentFeedback.set(null);
    this.officerQuery.set('');
    this.officerResults.set([]);
    this.selectedOfficer.set(null);
    this.qrScan()?.reset();
  }

  async confirmReturn(assignment: AssignmentSummary): Promise<void> {
    const alert = await this.alertController.create({
      header: 'Registrar devolución',
      message: `¿Registrar la devolución de ${assignment.codigoQr} por ${assignment.elementoNombre}? La fornitura volverá a disponible.`,
      buttons: [
        { text: 'Cancelar', role: 'cancel' },
        {
          text: 'Devolver',
          handler: () => {
            void this.doReturn(assignment);
          },
        },
      ],
    });
    await alert.present();
  }

  private async doReturn(assignment: AssignmentSummary): Promise<void> {
    try {
      await firstValueFrom(this.assignmentsService.returnAssignment(assignment.id));
      await this.showToast('Devolución registrada.', 'success');
      await this.loadVigentes();
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error), 'danger');
    }
  }

  prev(): void {
    if (this.page() > 0) {
      this.page.update((p) => p - 1);
      void this.loadVigentes();
    }
  }

  next(): void {
    if (this.page() + 1 < this.totalPages()) {
      this.page.update((p) => p + 1);
      void this.loadVigentes();
    }
  }

  private setEquipmentFeedback(
    title: string,
    message: string,
    color: EquipmentFeedbackColor,
  ): void {
    this.equipmentFeedback.set({ title, message, color });
  }

  private equipmentLookupErrorMessage(error: unknown, code: string): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 404) {
        return `No existe ninguna fornitura registrada con el código ${code}. Verifique el código escaneado o registre la fornitura antes de asignar.`;
      }
      if (error.status === 429) {
        return 'Demasiadas búsquedas seguidas. Espere un momento e intente de nuevo.';
      }
    }
    return extractApiErrorMessage(error, `No se pudo buscar la fornitura ${code}.`);
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
