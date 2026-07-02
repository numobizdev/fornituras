import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormControl, Validators } from '@angular/forms';
import { FieldErrorsComponent } from './field-errors.component';

@Component({
  template: '<app-field-errors [control]="control" [patternMessage]="patternMessage" />',
  imports: [FieldErrorsComponent],
})
class HostComponent {
  control = new FormControl('');
  patternMessage?: string;
}

/** 021 (FR-008/FR-010): mensajes de validación es-MX uniformes bajo cada campo. */
describe('FieldErrorsComponent', () => {
  const render = async (host: HostComponent, fixture = TestBed.createComponent(HostComponent)) => {
    Object.assign(fixture.componentInstance, host);
    fixture.detectChanges();
    await fixture.whenStable();
    return fixture;
  };

  const errorText = (fixture: { nativeElement: HTMLElement }): string | null =>
    fixture.nativeElement.querySelector('.field-error')?.textContent?.trim() ?? null;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [HostComponent] }).compileComponents();
  });

  it('renders nothing for a valid control', async () => {
    const fixture = TestBed.createComponent(HostComponent);
    fixture.componentInstance.control.setValue('ok');
    fixture.componentInstance.control.markAsTouched();
    fixture.detectChanges();
    expect(errorText(fixture)).toBeNull();
  });

  it('renders nothing while the control is untouched and pristine', async () => {
    const fixture = TestBed.createComponent(HostComponent);
    fixture.componentInstance.control.addValidators(Validators.required);
    fixture.componentInstance.control.updateValueAndValidity();
    fixture.detectChanges();
    expect(errorText(fixture)).toBeNull();
  });

  it('shows the required message once touched', async () => {
    const fixture = TestBed.createComponent(HostComponent);
    const control = fixture.componentInstance.control;
    control.addValidators(Validators.required);
    control.updateValueAndValidity();
    control.markAsTouched();
    fixture.detectChanges();
    expect(errorText(fixture)).toBe('Este campo es obligatorio.');
  });

  it('shows the email message', async () => {
    const fixture = TestBed.createComponent(HostComponent);
    const control = fixture.componentInstance.control;
    control.addValidators(Validators.email);
    control.setValue('no-es-correo');
    control.markAsTouched();
    fixture.detectChanges();
    expect(errorText(fixture)).toBe('Ingrese un correo válido.');
  });

  it('shows the maxlength message with the limit', async () => {
    const fixture = TestBed.createComponent(HostComponent);
    const control = fixture.componentInstance.control;
    control.addValidators(Validators.maxLength(3));
    control.setValue('demasiado');
    control.markAsTouched();
    fixture.detectChanges();
    expect(errorText(fixture)).toBe('Máximo 3 caracteres.');
  });

  it('shows the minlength message with the limit', async () => {
    const fixture = TestBed.createComponent(HostComponent);
    const control = fixture.componentInstance.control;
    control.addValidators(Validators.minLength(8));
    control.setValue('abc');
    control.markAsTouched();
    fixture.detectChanges();
    expect(errorText(fixture)).toBe('Mínimo 8 caracteres.');
  });

  it('uses patternMessage for pattern errors when provided', async () => {
    const fixture = TestBed.createComponent(HostComponent);
    fixture.componentInstance.patternMessage = 'La CURP debe tener 18 caracteres alfanuméricos.';
    const control = fixture.componentInstance.control;
    control.addValidators(Validators.pattern(/^[A-Za-z0-9]{18}$/));
    control.setValue('corta');
    control.markAsTouched();
    fixture.detectChanges();
    expect(errorText(fixture)).toBe('La CURP debe tener 18 caracteres alfanuméricos.');
  });

  it('falls back to a generic pattern message', async () => {
    const fixture = TestBed.createComponent(HostComponent);
    const control = fixture.componentInstance.control;
    control.addValidators(Validators.pattern(/^\d+$/));
    control.setValue('abc');
    control.markAsTouched();
    fixture.detectChanges();
    expect(errorText(fixture)).toBe('El formato no es válido.');
  });

  it('exposes the message as an alert for accessibility', async () => {
    const fixture = TestBed.createComponent(HostComponent);
    const control = fixture.componentInstance.control;
    control.addValidators(Validators.required);
    control.updateValueAndValidity();
    control.markAsTouched();
    fixture.detectChanges();
    const el = fixture.nativeElement.querySelector('.field-error');
    expect(el?.getAttribute('role')).toBe('alert');
  });
});
