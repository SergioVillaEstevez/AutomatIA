import { Component, inject, signal } from '@angular/core';
import { FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { problemMessage } from '../../core/problem';

@Component({
  selector: 'app-login',
  // FormsModule registra NgForm. Sin esa directiva, (ngSubmit) no existe y el
// navegador recarga la página con un envío nativo.
imports: [FormsModule, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './login.html',
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly email = new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] });
  readonly password = new FormControl('', { nonNullable: true, validators: [Validators.required] });
  readonly error = signal('');
  readonly submitting = signal(false);

  submit(): void {
    if (this.email.invalid || this.password.invalid || this.submitting()) {
      this.email.markAsTouched();
      this.password.markAsTouched();
      this.error.set('Introduce un correo y una clave válidos.');
      return;
    }
    this.submitting.set(true);
    this.error.set('');
    this.auth.login(this.email.value, this.password.value).subscribe({
      next: (response) => {
        const destination = response.user.passwordChangeRequired ? '/cambiar-clave' : '/';
        void this.router.navigateByUrl(destination);
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        this.error.set(problemMessage(error));
      },
    });
  }
}
