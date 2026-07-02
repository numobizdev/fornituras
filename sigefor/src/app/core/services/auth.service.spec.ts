import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { UserRole, UserSummary } from '../models/auth.model';
import { ROLE_POLICY } from '../security/role-policy';
import { AuthService } from './auth.service';

/** Autorización visual por capacidad (021): rechazo por defecto (FR-007). */
describe('AuthService.hasAnyRole', () => {
  let service: AuthService;

  const userWithRole = (role: UserRole): UserSummary => ({
    id: 1,
    name: 'Usuario Prueba',
    email: 'prueba@fornituras.local',
    role,
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    service = TestBed.inject(AuthService);
  });

  it('returns true when the current role is included', () => {
    service.currentUser.set(userWithRole('ALMACEN'));
    expect(service.hasAnyRole(ROLE_POLICY.WRITE_INVENTORY)).toBeTrue();
  });

  it('returns false when the current role is not included', () => {
    service.currentUser.set(userWithRole('AUDITOR'));
    expect(service.hasAnyRole(ROLE_POLICY.WRITE_INVENTORY)).toBeFalse();
  });

  it('returns false without a session (deny by default)', () => {
    service.currentUser.set(null);
    expect(service.hasAnyRole(ROLE_POLICY.MANAGE_CONFIG)).toBeFalse();
  });

  it('returns false for an unrecognized role value (deny by default)', () => {
    service.currentUser.set({ ...userWithRole('ADMIN'), role: 'OTRO' as UserRole });
    expect(service.hasAnyRole(ROLE_POLICY.WRITE_OPERATIONS)).toBeFalse();
  });
});
