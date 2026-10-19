import { provideZonelessChangeDetection, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { authGuard, guestGuard } from './auth.guard';
import { AuthService } from './auth.service';

describe('auth guards', () => {
  const authenticated = signal(false);

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        { provide: AuthService, useValue: { isAuthenticated: authenticated } },
      ],
    });
  });

  const run = (guard: typeof authGuard) =>
    TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );

  it('redirects anonymous users to the login', () => {
    authenticated.set(false);
    const result = run(authGuard) as UrlTree;
    expect(result.toString()).toBe('/login');
  });

  it('lets authenticated users in', () => {
    authenticated.set(true);
    expect(run(authGuard)).toBeTrue();
  });

  it('sends logged-in users from the login to the dashboard', () => {
    authenticated.set(true);
    expect((run(guestGuard) as UrlTree).toString()).toBe('/uebersicht');
  });
});
