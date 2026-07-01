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
import { UserRole } from '../../../../core/models/auth.model';
import { extractApiErrorMessage } from '../../../../core/utils/api-error.util';
import { ROLE_OPTIONS } from '../../data/role-options';
import { UsersService } from '../../data/users.service';

@Component({
  selector: 'app-usuario-form',
  templateUrl: './usuario-form.page.html',
  styleUrls: ['./usuario-form.page.scss'],
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
  ],
})
export class UsuarioFormPage implements OnInit {
  private readonly service = inject(UsersService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toastController = inject(ToastController);

  readonly roleOptions = ROLE_OPTIONS;

  readonly userId = signal<number | null>(null);
  readonly isLoading = signal(false);
  readonly isSubmitting = signal(false);

  private originalRole: UserRole | null = null;

  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    role: ['CAPTURISTA' as UserRole, [Validators.required]],
  });

  get isEditing(): boolean {
    return this.userId() !== null;
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.userId.set(Number(idParam));
      this.form.controls.email.disable();
      void this.loadDetail(Number(idParam));
    }
  }

  private async loadDetail(id: number): Promise<void> {
    this.isLoading.set(true);
    try {
      const user = await firstValueFrom(this.service.getById(id));
      this.originalRole = user.role;
      this.form.patchValue({ name: user.name, email: user.email, role: user.role });
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
    try {
      const id = this.userId();
      if (id !== null) {
        await firstValueFrom(this.service.update(id, { name: value.name.trim() }));
        if (value.role !== this.originalRole) {
          await firstValueFrom(this.service.changeRole(id, value.role));
        }
        await this.showToast('Usuario actualizado.', 'success');
      } else {
        await firstValueFrom(
          this.service.create({
            name: value.name.trim(),
            email: value.email.trim(),
            role: value.role,
          }),
        );
        await this.showToast('Usuario creado. Se envió un código de activación al correo.', 'success');
      }
      await this.router.navigate(['/usuarios']);
    } catch (error) {
      await this.showToast(extractApiErrorMessage(error, 'No se pudo guardar el usuario.'), 'danger');
    } finally {
      this.isSubmitting.set(false);
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
