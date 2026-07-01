import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AppComponent } from './app.component';
import { UserSummary } from './core/models/auth.model';
import { AuthService } from './core/services/auth.service';

/**
 * El shell autenticado (menú lateral) debe montarse SOLO con sesión válida, derivándolo del estado de
 * sesión y no de la URL (FR-016 / T031b). Sin sesión, un visitante no ve el menú en ningún momento.
 */
describe('AppComponent', () => {
  const authStub = {
    currentUser: signal<UserSummary | null>(null),
    isAuthenticated: signal(false),
    logout: async () => {},
  };

  beforeEach(async () => {
    authStub.currentUser.set(null);
    authStub.isAuthenticated.set(false);

    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [provideRouter([]), { provide: AuthService, useValue: authStub }],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('does not render the navigation menu without a session', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    const host = fixture.nativeElement as HTMLElement;
    expect(host.querySelector('ion-menu')).toBeNull();
    expect(host.querySelectorAll('ion-label').length).toEqual(0);
  });

  it('renders the shell with role-filtered items when authenticated', async () => {
    authStub.isAuthenticated.set(true);
    authStub.currentUser.set({ id: 1, name: 'Admin', email: 'a@b.mx', role: 'ADMIN' });

    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    const host = fixture.nativeElement as HTMLElement;
    expect(host.querySelector('ion-menu')).not.toBeNull();

    const labels = Array.from(host.querySelectorAll('ion-label')).map((el) => el.textContent ?? '');
    expect(labels.some((t) => t.includes('Inicio'))).toBeTrue();
    // Entrada solo-ADMIN visible para el rol ADMIN.
    expect(labels.some((t) => t.includes('Contenido de bienvenida'))).toBeTrue();
  });

  it('hides ADMIN-only items for non-admin roles', async () => {
    authStub.isAuthenticated.set(true);
    authStub.currentUser.set({ id: 2, name: 'Cap', email: 'c@b.mx', role: 'CAPTURISTA' });

    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    const host = fixture.nativeElement as HTMLElement;
    const labels = Array.from(host.querySelectorAll('ion-label')).map((el) => el.textContent ?? '');
    expect(labels.some((t) => t.includes('Inicio'))).toBeTrue();
    expect(labels.some((t) => t.includes('Contenido de bienvenida'))).toBeFalse();
    expect(labels.some((t) => t.includes('Usuarios'))).toBeFalse();
  });
});
