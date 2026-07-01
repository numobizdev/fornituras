import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import {
  ActionSheetController,
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
  IonNote,
  IonSegment,
  IonSegmentButton,
  IonSpinner,
  IonTitle,
  IonToolbar,
  ToastController,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import { add, createOutline, powerOutline, ribbonOutline } from 'ionicons/icons';
import { UserRole } from '../../../../core/models/auth.model';
import { extractApiErrorMessage } from '../../../../core/utils/api-error.util';
import { ROLE_LABELS, ROLE_OPTIONS } from '../../data/role-options';
import { UserSummary } from '../../data/user.model';
import { UsersService } from '../../data/users.service';

@Component({
  selector: 'app-usuarios',
  templateUrl: './usuarios.page.html',
  styleUrls: ['./usuarios.page.scss'],
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
    IonNote,
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
export class UsuariosPage {
  private readonly service = inject(UsersService);
  private readonly router = inject(Router);
  private readonly toastController = inject(ToastController);
  private readonly alertController = inject(AlertController);
  private readonly actionSheetController = inject(ActionSheetController);

  readonly roleLabels = ROLE_LABELS;

  private readonly allUsers = signal<UserSummary[]>([]);
  readonly isLoading = signal(false);
  readonly filter = signal<'active' | 'inactive'>('active');

  readonly users = computed(() =>
    this.allUsers().filter((user) => user.enabled === (this.filter() === 'active')),
  );

  constructor() {
    addIcons({ add, createOutline, powerOutline, ribbonOutline });
  }

  // Recarga en cada entrada (Ionic cachea la página y no re-ejecuta ngOnInit al volver del formulario).
  ionViewWillEnter(): void {
    void this.load();
  }

  async load(): Promise<void> {
    this.isLoading.set(true);
    try {
      const page = await firstValueFrom(this.service.list({ size: 100 }));
      this.allUsers.set(page.content);
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error), 'danger');
    } finally {
      this.isLoading.set(false);
    }
  }

  onFilterChange(value: string | undefined): void {
    if (value === 'active' || value === 'inactive') {
      this.filter.set(value);
    }
  }

  goToNew(): void {
    void this.router.navigate(['/usuarios/nuevo']);
  }

  goToEdit(user: UserSummary): void {
    void this.router.navigate(['/usuarios', user.id]);
  }

  async confirmToggleEnabled(user: UserSummary): Promise<void> {
    const alert = await this.alertController.create({
      header: user.enabled ? 'Desactivar usuario' : 'Activar usuario',
      message: user.enabled
        ? `¿Desactivar a "${user.name}"? No podrá iniciar sesión hasta reactivarlo.`
        : `¿Reactivar a "${user.name}"?`,
      buttons: [
        { text: 'Cancelar', role: 'cancel' },
        {
          text: user.enabled ? 'Desactivar' : 'Activar',
          role: user.enabled ? 'destructive' : undefined,
          handler: () => {
            void this.toggleEnabled(user);
          },
        },
      ],
    });
    await alert.present();
  }

  async changeRole(user: UserSummary): Promise<void> {
    const sheet = await this.actionSheetController.create({
      header: `Rol de ${user.name}`,
      buttons: [
        ...ROLE_OPTIONS.map((option) => ({
          text: option.value === user.role ? `${option.label} (actual)` : option.label,
          handler: () => {
            if (option.value !== user.role) {
              void this.applyRole(user, option.value);
            }
          },
        })),
        { text: 'Cancelar', role: 'cancel' as const },
      ],
    });
    await sheet.present();
  }

  private async toggleEnabled(user: UserSummary): Promise<void> {
    try {
      await firstValueFrom(this.service.setEnabled(user.id, !user.enabled));
      await this.showToast(user.enabled ? 'Usuario desactivado.' : 'Usuario activado.', 'success');
      await this.load();
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error), 'danger');
    }
  }

  private async applyRole(user: UserSummary, role: UserRole): Promise<void> {
    try {
      await firstValueFrom(this.service.changeRole(user.id, role));
      await this.showToast('Rol actualizado.', 'success');
      await this.load();
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error), 'danger');
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
