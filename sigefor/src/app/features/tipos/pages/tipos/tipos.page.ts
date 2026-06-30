import { Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
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
  IonItem,
  IonLabel,
  IonList,
  IonMenuButton,
  IonSegment,
  IonSegmentButton,
  IonSpinner,
  IonTitle,
  IonToolbar,
  ToastController,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import { add, createOutline, powerOutline } from 'ionicons/icons';
import { AuthService } from '../../../../core/services/auth.service';
import { extractApiErrorMessage } from '../../../../core/utils/api-error.util';
import { EquipmentTypesService } from '../../data/equipment-types.service';
import { EquipmentTypeSummary } from '../../data/equipment-type.model';

@Component({
  selector: 'app-tipos',
  templateUrl: './tipos.page.html',
  styleUrls: ['./tipos.page.scss'],
  imports: [
    IonHeader,
    IonToolbar,
    IonButtons,
    IonMenuButton,
    IonTitle,
    IonContent,
    IonSegment,
    IonSegmentButton,
    IonLabel,
    IonList,
    IonItem,
    IonBadge,
    IonButton,
    IonIcon,
    IonSpinner,
    IonFab,
    IonFabButton,
  ],
})
export class TiposPage implements OnInit {
  private readonly service = inject(EquipmentTypesService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toastController = inject(ToastController);
  private readonly alertController = inject(AlertController);

  readonly types = signal<EquipmentTypeSummary[]>([]);
  readonly isLoading = signal(false);
  readonly filter = signal<'active' | 'inactive'>('active');
  readonly isAdmin = this.auth.hasRole('ADMIN');

  constructor() {
    addIcons({ add, createOutline, powerOutline });
  }

  ngOnInit(): void {
    void this.load();
  }

  async load(): Promise<void> {
    this.isLoading.set(true);
    try {
      const page = await firstValueFrom(
        this.service.list({ active: this.filter() === 'active', size: 100 }),
      );
      this.types.set(page.content);
    } catch (error) {
      await this.showError(extractApiErrorMessage(error));
    } finally {
      this.isLoading.set(false);
    }
  }

  onFilterChange(value: string | undefined): void {
    if (value === 'active' || value === 'inactive') {
      this.filter.set(value);
      void this.load();
    }
  }

  goToNew(): void {
    void this.router.navigate(['/tipos/nuevo']);
  }

  goToEdit(type: EquipmentTypeSummary): void {
    void this.router.navigate(['/tipos', type.id]);
  }

  async confirmDeactivate(type: EquipmentTypeSummary): Promise<void> {
    const alert = await this.alertController.create({
      header: 'Desactivar tipo',
      message: `¿Desactivar "${type.nombre}"? Dejará de ofrecerse en el alta de fornituras, pero no se borra.`,
      buttons: [
        { text: 'Cancelar', role: 'cancel' },
        {
          text: 'Desactivar',
          role: 'destructive',
          handler: () => {
            void this.deactivate(type);
          },
        },
      ],
    });
    await alert.present();
  }

  private async deactivate(type: EquipmentTypeSummary): Promise<void> {
    try {
      await firstValueFrom(this.service.deactivate(type.id));
      await this.showToast('Tipo desactivado.', 'success');
      await this.load();
    } catch (error) {
      await this.showError(extractApiErrorMessage(error));
    }
  }

  private async showError(message: string): Promise<void> {
    await this.showToast(message, 'danger');
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
