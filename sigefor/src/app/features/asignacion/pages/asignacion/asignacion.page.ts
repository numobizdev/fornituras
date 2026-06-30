import { Component, inject, OnInit, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import {
  AlertController,
  IonBadge,
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
  IonMenuButton,
  IonNote,
  IonSearchbar,
  IonSpinner,
  IonText,
  IonTitle,
  IonToolbar,
  ToastController,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import { checkmarkCircle, closeCircle, searchOutline } from 'ionicons/icons';
import { AuthService } from '../../../../core/services/auth.service';
import { extractApiErrorMessage } from '../../../../core/utils/api-error.util';
import { EquipmentService } from '../../../fornituras/data/equipment.service';
import { EquipmentDetail } from '../../../fornituras/data/equipment.model';
import { OfficersService } from '../../../elementos/data/officers.service';
import { OfficerSummary } from '../../../elementos/data/officer.model';
import { AssignmentsService } from '../../data/assignments.service';
import { AssignmentSummary } from '../../data/assignment.model';

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
    IonList,
    IonListHeader,
    IonItem,
    IonLabel,
    IonNote,
    IonText,
    IonInput,
    IonSearchbar,
    IonBadge,
    IonButton,
    IonIcon,
    IonSpinner,
  ],
})
export class AsignacionPage implements OnInit {
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

  // Paso 2: elemento
  readonly officerQuery = signal('');
  readonly searchingOfficer = signal(false);
  readonly officerResults = signal<OfficerSummary[]>([]);
  readonly selectedOfficer = signal<OfficerSummary | null>(null);

  readonly isSubmitting = signal(false);
  readonly canAssign = this.auth.hasRole('ADMIN') || this.auth.hasRole('CAPTURISTA');

  constructor() {
    addIcons({ checkmarkCircle, closeCircle, searchOutline });
  }

  ngOnInit(): void {
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

  async resolveEquipment(): Promise<void> {
    const code = this.codigo().trim();
    if (!code) {
      return;
    }
    this.resolvingEquipment.set(true);
    this.equipment.set(null);
    try {
      this.equipment.set(await firstValueFrom(this.equipmentService.getByCodigo(code)));
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error, 'No se encontró la fornitura.'), 'danger');
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
    this.officerQuery.set('');
    this.officerResults.set([]);
    this.selectedOfficer.set(null);
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
