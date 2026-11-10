import { Component, inject, signal } from '@angular/core';
import { FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { problemMessage } from '../../core/problem';

@Component({
  selector: 'app-change-password',
  imports: [FormsModule, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './change-password.html',
})
export class ChangePasswordPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly currentPassword = new FormControl('', { nonNullable: true, validators: [Validators.required] });
  readonly newPassword = new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.minLength(12)] });
  readonly error = signal('');
  readonly submitting = signal(false);

  submit(): void {
    if (this.currentPassword.invalid || this.newPassword.invalid || this.submitting()) {
      this.error.set('La clave nueva necesita al menos 12 caracteres.');
      return;
    }
    this.submitting.set(true);
    this.error.set('');
    this.auth.changePassword(this.currentPassword.value, this.newPassword.value).subscribe({
      next: () => void this.router.navigateByUrl('/'),
      error: (error: unknown) => {
        this.submitting.set(false);
        this.error.set(problemMessage(error));
      },
    });
  }
}
