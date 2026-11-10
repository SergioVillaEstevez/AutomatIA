import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-home-redirect',
  template: '',
})
export class HomeRedirect {
  constructor() {
    const role = inject(AuthService).user()?.role;
    void inject(Router).navigateByUrl(role === 'SUPER_ADMIN' ? '/admin' : '/empresa');
  }
}
