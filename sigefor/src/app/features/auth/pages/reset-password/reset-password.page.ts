import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import {
  IonButton,
  IonContent,
  IonInput,
  IonInputPasswordToggle,
  IonItem,
  IonSpinner,
  IonText,
  ToastController,
} from '@ionic/angular/standalone';
import { AuthService } from '../../../../core/services/auth.service';
import { extractApiErrorMessage } from '../../../../core/utils/api-error.util';

@Component({
  selector: 'app-reset-password',
  templateUrl: './reset-password.page.html',
  styleUrls: ['../../styles/auth-page.scss'],
  imports: [
    ReactiveFormsModule,
    RouterLink,
    IonContent,
    IonItem,
    IonInput,
    IonInputPasswordToggle,
    IonButton,
    IonSpinner,
    IonText,
  ],
})
export class ResetPasswordPage {
  private readonly authService = inject(AuthService);
  private readonly toastController = inject(ToastController);
  private readonly formBuilder = inject(FormBuilder);

  readonly isSubmitting = signal(false);
  readonly resetComplete = signal(false);

  readonly form = this.formBuilder.nonNullable.group({
    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
    newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]],
    confirmPassword: ['', [Validators.required]],
  });

  async onSubmit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { code, newPassword, confirmPassword } = this.form.getRawValue();
    if (newPassword !== confirmPassword) {
      await this.showError('Las contraseñas no coinciden.');
      return;
    }

    this.isSubmitting.set(true);

    try {
      const response = await firstValueFrom(
        this.authService.resetPassword({ code, newPassword }),
      );

      if (!response?.success) {
        await this.showError(response?.message ?? 'No se pudo restablecer la contraseña.');
        return;
      }

      this.resetComplete.set(true);
    } catch (error) {
      await this.showError(
        extractApiErrorMessage(error, 'Código inválido o expirado. Solicite uno nuevo.'),
      );
    } finally {
      this.isSubmitting.set(false);
    }
  }

  private async showError(message: string): Promise<void> {
    const toast = await this.toastController.create({
      message,
      duration: 4000,
      color: 'danger',
      position: 'top',
    });
    await toast.present();
  }
}
