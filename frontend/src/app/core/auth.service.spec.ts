import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { CurrentUser } from './models';

describe('AuthService', () => {
  const user: CurrentUser = {
    id: '1',
    email: 'admin@neiver.local',
    firstName: 'Admin',
    lastName: 'Local',
    role: 'SUPER_ADMIN',
    passwordChangeRequired: true,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
  });

  it('conserva el token de acceso en memoria y no lo confunde con la cookie', () => {
    const auth = TestBed.inject(AuthService);
    const http = TestBed.inject(HttpTestingController);

    auth.login('admin@neiver.local', 'Neiver-Dev-Admin-12').subscribe();
    const request = http.expectOne('/api/v1/auth/login');
    expect(request.request.withCredentials).toBe(true);
    request.flush({
      accessToken: 'token-de-acceso',
      accessTokenExpiresAt: '2026-03-01T10:15:00Z',
      user,
    });

    expect(auth.token()).toBe('token-de-acceso');
    expect(auth.user()?.passwordChangeRequired).toBe(true);
    http.verify();
  });

  it('olvida el token al cerrar sesión aunque la petición falle', () => {
    const auth = TestBed.inject(AuthService);
    const http = TestBed.inject(HttpTestingController);
    auth.login('admin@neiver.local', 'Neiver-Dev-Admin-12').subscribe();
    http.expectOne('/api/v1/auth/login').flush({
      accessToken: 'token-de-acceso',
      accessTokenExpiresAt: '2026-03-01T10:15:00Z',
      user,
    });

    auth.logout().subscribe({ error: () => undefined });
    http.expectOne('/api/v1/auth/logout').flush('no', { status: 500, statusText: 'Error' });
    expect(auth.token()).toBeNull();
  });
});
