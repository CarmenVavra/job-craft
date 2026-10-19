import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/** Hängt das JWT an API-Aufrufe und meldet bei abgelaufener Sitzung (401) ab. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token();
  const isAuthCall =
    req.url.startsWith('/api/auth/login') || req.url.startsWith('/api/auth/register');
  const request =
    token && req.url.startsWith('/api/') && !isAuthCall
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;
  return next(request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && !isAuthCall) {
        auth.logout();
      }
      return throwError(() => error);
    }),
  );
};
