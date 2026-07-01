import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../core/models/api-response.model';
import { UserRole } from '../../../core/models/auth.model';
import { Page, UserCreateRequest, UserSummary, UserUpdateRequest } from './user.model';

@Injectable({ providedIn: 'root' })
export class UsersService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/users`;

  list(options: { page?: number; size?: number } = {}): Observable<Page<UserSummary>> {
    let params = new HttpParams();
    if (options.page !== undefined) {
      params = params.set('page', options.page);
    }
    if (options.size !== undefined) {
      params = params.set('size', options.size);
    }
    return this.http
      .get<ApiResponse<Page<UserSummary>>>(this.baseUrl, { params })
      .pipe(map((response) => response.data));
  }

  getById(id: number): Observable<UserSummary> {
    return this.http
      .get<ApiResponse<UserSummary>>(`${this.baseUrl}/${id}`)
      .pipe(map((response) => response.data));
  }

  create(request: UserCreateRequest): Observable<UserSummary> {
    return this.http
      .post<ApiResponse<UserSummary>>(this.baseUrl, request)
      .pipe(map((response) => response.data));
  }

  update(id: number, request: UserUpdateRequest): Observable<UserSummary> {
    return this.http
      .put<ApiResponse<UserSummary>>(`${this.baseUrl}/${id}`, request)
      .pipe(map((response) => response.data));
  }

  setEnabled(id: number, enabled: boolean): Observable<UserSummary> {
    return this.http
      .patch<ApiResponse<UserSummary>>(`${this.baseUrl}/${id}/enabled`, { enabled })
      .pipe(map((response) => response.data));
  }

  changeRole(id: number, role: UserRole): Observable<UserSummary> {
    return this.http
      .patch<ApiResponse<UserSummary>>(`${this.baseUrl}/${id}/role`, { role })
      .pipe(map((response) => response.data));
  }
}
