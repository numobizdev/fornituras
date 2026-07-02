export type UserRole =
  | 'ADMIN'
  | 'SUPER_ADMIN'
  | 'SUPERVISOR'
  | 'ALMACEN'
  | 'AUDITOR'
  | 'CAPTURISTA';

export interface UserSummary {
  id: number;
  name: string;
  email: string;
  role: UserRole;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  user: UserSummary;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  code: string;
  newPassword: string;
}

export interface StoredSession {
  token: string;
  tokenType: string;
  expiresAt: number;
  user: UserSummary;
}
