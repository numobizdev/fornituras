import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import {
  IonButton,
  IonContent,
  IonInput,
  IonItem,
  IonSpinner,
  IonText,
  ToastController,
} from '@ionic/angular/standalone';
import { AuthService } from '../../../../core/services/auth.service';
import { extractApiErrorMessage } from '../../../../core/utils/api-error.util';

@Component({
  selector: 'app-forgot-password',
  templateUrl: './forgot-password.page.html',
  styleUrls: ['../../styles/auth-page.scss'],
  imports: [
    ReactiveFormsModule,
    RouterLink,
    IonContent,
    IonItem,
    IonInput,
    IonButton,
    IonSpinner,
    IonText,
  ],
})
export class ForgotPasswordPage {
  private readonly authService = inject(AuthService);
  private readonly toastController = inject(ToastController);
  private readonly formBuilder = inject(FormBuilder);

  readonly isSubmitting = signal(false);
  readonly emailSent = signal(false);

  readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  async onSubmit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);

    try {
      const response = await firstValueFrom(
        this.authService.forgotPassword(this.form.getRawValue()),
      );

      if (!response?.success) {
        await this.showError(response?.message ?? 'No se pudo enviar el código.');
        return;
      }

      this.emailSent.set(true);
    } catch (error) {
      await this.showError(extractApiErrorMessage(error, 'No se pudo enviar el código.'));
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
