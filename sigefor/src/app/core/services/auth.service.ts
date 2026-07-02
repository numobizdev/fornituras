import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import {
  AuthResponse,
  ForgotPasswordRequest,
  LoginRequest,
  ResetPasswordRequest,
  StoredSession,
  UserSummary,
} from '../models/auth.model';
import { TokenStorageService } from './token-storage.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly tokenStorage = inject(TokenStorageService);

  private readonly sessionSignal = signal<StoredSession | null>(null);

  readonly currentUser = signal<UserSummary | null>(null);
  readonly isAuthenticated = signal(false);

  async restoreSession(): Promise<void> {
    const session = await this.tokenStorage.loadSession();
    if (!session || this.isSessionExpired(session)) {
      await this.clearSession();
      return;
    }

    this.applySession(session);
  }

  login(request: LoginRequest) {
    return this.http.post<ApiResponse<AuthResponse>>(
      `${environment.apiUrl}/auth/login`,
      request,
    );
  }

  async handleLoginSuccess(response: AuthResponse): Promise<void> {
    const session: StoredSession = {
      token: response.token,
      tokenType: response.tokenType,
      expiresAt: Date.now() + response.expiresIn,
      user: response.user,
    };

    await this.tokenStorage.saveSession(session);
    this.applySession(session);
  }

  forgotPassword(request: ForgotPasswordRequest) {
    return this.http.post<ApiResponse<null>>(
      `${environment.apiUrl}/auth/forgot-password`,
      request,
    );
  }

  resetPassword(request: ResetPasswordRequest) {
    return this.http.post<ApiResponse<null>>(
      `${environment.apiUrl}/auth/reset-password`,
      request,
    );
  }

  getToken(): string | null {
    return this.sessionSignal()?.token ?? null;
  }

  hasRole(role: UserSummary['role']): boolean {
    return this.currentUser()?.role === role;
  }

  /** Destino tras login según rol (SUPER_ADMIN → módulo QR). */
  getPostLoginRoute(): string {
    return this.hasRole('SUPER_ADMIN') ? '/qr-lotes' : '/inicio';
  }

  /** Autorización visual por capacidad (espejo de RolePolicy.cs): rechazo por defecto. */
  hasAnyRole(roles: ReadonlyArray<UserSummary['role']>): boolean {
    const role = this.currentUser()?.role;
    return role != null && roles.includes(role);
  }

  async logout(redirectToLogin = true): Promise<void> {
    await this.clearSession();
    if (redirectToLogin) {
      await this.router.navigate(['/login']);
    }
  }

  private applySession(session: StoredSession): void {
    this.sessionSignal.set(session);
    this.currentUser.set(session.user);
    this.isAuthenticated.set(true);
  }

  private async clearSession(): Promise<void> {
    await this.tokenStorage.clearSession();
    this.sessionSignal.set(null);
    this.currentUser.set(null);
    this.isAuthenticated.set(false);
  }

  private isSessionExpired(session: StoredSession): boolean {
    return Date.now() >= session.expiresAt;
  }

  async ensureAuthenticated(): Promise<boolean> {
    if (this.isAuthenticated()) {
      const session = this.sessionSignal();
      if (session && !this.isSessionExpired(session)) {
        return true;
      }
    }

    await this.restoreSession();
    return this.isAuthenticated();
  }
}
