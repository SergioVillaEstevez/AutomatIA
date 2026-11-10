import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from './auth.service';
import { Role } from './models';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.ensure().pipe(map((user) => {
    if (!user) {
      return router.parseUrl('/login');
    }
    const changingPassword = state.url.startsWith('/cambiar-clave');
    if (user.passwordChangeRequired && !changingPassword) {
      return router.parseUrl('/cambiar-clave');
    }
    if (!user.passwordChangeRequired && changingPassword) {
      return router.parseUrl('/');
    }
    return true;
  }));
};

export function roleGuard(...roles: Role[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    const role = auth.user()?.role;
    if (role && roles.includes(role)) {
      return true;
    }
    return router.parseUrl('/');
  };
}

export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.ensure().pipe(map((user) => {
    if (!user) {
      return true;
    }
    return router.parseUrl(user.passwordChangeRequired ? '/cambiar-clave' : '/');
  }));
};
