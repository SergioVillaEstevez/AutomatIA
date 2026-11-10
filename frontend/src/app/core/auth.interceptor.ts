import { HttpContextToken, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

const RETRIED = new HttpContextToken<boolean>(() => false);

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.token();
  let headers = request.headers;
  if (token) {
    headers = headers.set('Authorization', `Bearer ${token}`);
  }
  const outgoing = request.clone({ headers, withCredentials: true });

  return next(outgoing).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse)) {
        return throwError(() => error);
      }
      if (error.status === 403 && error.error?.code === 'PASSWORD_CHANGE_REQUIRED') {
        void router.navigateByUrl('/cambiar-clave');
      }
      const isAuthCall = request.url.includes('/api/v1/auth/');
      if (error.status !== 401 || isAuthCall || request.context.get(RETRIED)) {
        return throwError(() => error);
      }
      return auth.refresh().pipe(
        switchMap(() => {
          const renewed = auth.token();
          if (!renewed) {
            auth.clear();
            void router.navigateByUrl('/login');
            return throwError(() => error);
          }
          return next(request.clone({
            headers: request.headers.set('Authorization', `Bearer ${renewed}`),
            context: request.context.set(RETRIED, true),
            withCredentials: true,
          }));
        }),
        catchError((refreshError: unknown) => {
          auth.clear();
          void router.navigateByUrl('/login');
          return throwError(() => refreshError);
        }),
      );
    }),
  );
};
