import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, finalize, map, of, shareReplay, tap } from 'rxjs';
import { AuthResponse, CurrentUser } from './models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly session = signal<CurrentUser | null>(null);
  private accessToken: string | null = null;
  private restoreRequest: Observable<CurrentUser | null> | null = null;

  readonly user = this.session.asReadonly();

  token(): string | null {
    return this.accessToken;
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/v1/auth/login', { email, password }, { withCredentials: true })
      .pipe(tap((response) => this.remember(response)));
  }

  changePassword(currentPassword: string, newPassword: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/v1/auth/change-password', { currentPassword, newPassword })
      .pipe(tap((response) => this.remember(response)));
  }

  refresh(): Observable<CurrentUser | null> {
    return this.http.post<AuthResponse>('/api/v1/auth/refresh', null, { withCredentials: true }).pipe(
      tap((response) => this.remember(response)),
      map((response) => response.user),
    );
  }

  ensure(): Observable<CurrentUser | null> {
    if (this.session()) {
      return of(this.session());
    }
    if (!this.restoreRequest) {
      this.restoreRequest = this.refresh().pipe(
        catchError(() => {
          this.clear();
          return of(null);
        }),
        finalize(() => {
          this.restoreRequest = null;
        }),
        shareReplay(1),
      );
    }
    return this.restoreRequest;
  }

  logout(): Observable<void> {
    return this.http.post<void>('/api/v1/auth/logout', null, { withCredentials: true }).pipe(
      finalize(() => this.clear()),
    );
  }

  clear(): void {
    this.accessToken = null;
    this.session.set(null);
  }

  private remember(response: AuthResponse): void {
    this.accessToken = response.accessToken;
    this.session.set(response.user);
  }
}
