import { Component, input } from '@angular/core';
import { AbstractControl } from '@angular/forms';

const MESSAGES: Record<string, (error: unknown) => string> = {
  required: () => 'Este campo es obligatorio.',
  email: () => 'Ingrese un correo válido.',
  maxlength: (e) => `Máximo ${(e as { requiredLength: number }).requiredLength} caracteres.`,
  minlength: (e) => `Mínimo ${(e as { requiredLength: number }).requiredLength} caracteres.`,
  min: (e) => `El valor mínimo es ${(e as { min: number }).min}.`,
  max: (e) => `El valor máximo es ${(e as { max: number }).max}.`,
  pattern: () => 'El formato no es válido.',
};

/**
 * Mensaje de validación es-MX bajo un campo (021, FR-008): muestra el primer error activo
 * cuando el control fue tocado o modificado. Uso:
 * `<app-field-errors [control]="form.controls.x" [patternMessage]="'…'" />`
 */
// Sin OnPush a propósito: el estado touched/errors del control muta sin cambiar la
// referencia del input, y el mensaje debe refrescarse con la detección del padre.
@Component({
  selector: 'app-field-errors',
  template: `
    @if (message(); as text) {
      <span class="field-error" role="alert" aria-live="polite">{{ text }}</span>
    }
  `,
})
export class FieldErrorsComponent {
  readonly control = input.required<AbstractControl>();
  /** Mensaje específico para errores `pattern` (p. ej. formato de CURP/RFC). */
  readonly patternMessage = input<string>();

  message(): string | null {
    const control = this.control();
    if (control.valid || (!control.touched && !control.dirty) || !control.errors) {
      return null;
    }

    const [key, error] = Object.entries(control.errors)[0];
    if (key === 'pattern' && this.patternMessage()) {
      return this.patternMessage() ?? null;
    }
    return MESSAGES[key]?.(error) ?? 'El valor no es válido.';
  }
}
