import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AuthService } from './auth.service';

/** Erzeugt ein unsigniertes JWT mit Ablaufzeit (nur für Tests). */
function token(expSeconds: number): string {
  const payload = btoa(JSON.stringify({ sub: 'u1', exp: expSeconds }));
  return `header.${payload}.signature`;
}

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('stores the session after login', () => {
    const jwt = token(Date.now() / 1000 + 3600);
    service.login({ email: 'lena@example.de', password: 'geheim123' }).subscribe();

    const req = http.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush({ token: jwt, user: { id: 'u1', email: 'lena@example.de', name: 'Lena Hoffmann' } });

    expect(service.isAuthenticated()).toBeTrue();
    expect(service.user()?.name).toBe('Lena Hoffmann');
    expect(localStorage.getItem('jobcraft.token')).toBe(jwt);
  });

  it('treats expired tokens as logged out', () => {
    service.register({ name: 'Lena', email: 'lena@example.de', password: 'geheim123' }).subscribe();
    http.expectOne('/api/auth/register').flush({
      token: token(Date.now() / 1000 - 10),
      user: { id: 'u1', email: 'lena@example.de', name: 'Lena' },
    });

    expect(service.isAuthenticated()).toBeFalse();
  });

  it('clears the session on logout and navigates to login', () => {
    const router = TestBed.inject(Router);
    const navigate = spyOn(router, 'navigate').and.resolveTo(true);
    service.login({ email: 'lena@example.de', password: 'geheim123' }).subscribe();
    http.expectOne('/api/auth/login').flush({
      token: token(Date.now() / 1000 + 3600),
      user: { id: 'u1', email: 'lena@example.de', name: 'Lena' },
    });

    service.logout();

    expect(service.token()).toBeNull();
    expect(localStorage.getItem('jobcraft.token')).toBeNull();
    expect(navigate).toHaveBeenCalledWith(['/login']);
  });

  it('sends password changes to the API', () => {
    service.changePassword({ currentPassword: 'alt12345', newPassword: 'neu123456' }).subscribe();
    const req = http.expectOne('/api/auth/password');
    expect(req.request.method).toBe('PUT');
    req.flush(null);
  });
});
