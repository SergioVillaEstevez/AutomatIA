import { Component, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

@Component({
  selector: 'app-suspend-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Suspender empresa</h2>
    <mat-dialog-content>
      <p class="help">Las sesiones abiertas de esa empresa dejarán de valer. Indica el motivo, quedará en la auditoría.</p>
      <mat-form-field appearance="outline" class="span-2">
        <mat-label>Motivo</mat-label>
        <textarea matInput rows="3" [formControl]="reason"></textarea>
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" (click)="close()">Cancelar</button>
      <button mat-flat-button type="button" [disabled]="reason.invalid" (click)="confirm()">Suspender</button>
    </mat-dialog-actions>
  `,
})
export class SuspendDialog {
  private readonly dialog = inject(MatDialogRef<SuspendDialog, string>);
  readonly reason = new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(500)] });

  close(): void { this.dialog.close(); }
  confirm(): void { if (this.reason.valid) this.dialog.close(this.reason.value.trim()); }
}

@Component({
  selector: 'app-user-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatSelectModule],
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      <p class="help">{{ data.help }}</p>
      @if (data.kind === 'create') {
        <div class="stack">
          <mat-form-field appearance="outline"><mat-label>Nombre</mat-label><input matInput [formControl]="firstName"></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Apellidos</mat-label><input matInput [formControl]="lastName"></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Correo</mat-label><input matInput type="email" [formControl]="email"></mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Rol</mat-label>
            <mat-select [formControl]="role">
              <mat-option value="COMPANY_ADMIN">Administrador de empresa</mat-option>
              <mat-option value="COMPANY_USER">Usuario</mat-option>
            </mat-select>
          </mat-form-field>
        </div>
      }
      <mat-form-field appearance="outline" class="span-2">
        <mat-label>Clave temporal</mat-label>
        <input matInput type="password" autocomplete="new-password" [formControl]="password">
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" (click)="close()">Cancelar</button>
      <button mat-flat-button type="button" [disabled]="invalid()" (click)="confirm()">Guardar</button>
    </mat-dialog-actions>
  `,
})
export class UserDialog {
  readonly data = inject<UserDialogData>(MAT_DIALOG_DATA);
  private readonly dialog = inject(MatDialogRef<UserDialog, UserDialogResult>);
  readonly firstName = new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(80)] });
  readonly lastName = new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(120)] });
  readonly email = new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] });
  readonly role = new FormControl('COMPANY_USER', { nonNullable: true, validators: [Validators.required] });
  readonly password = new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.minLength(12), Validators.maxLength(200)] });

  invalid(): boolean {
    if (this.password.invalid) return true;
    return this.data.kind === 'create' && (this.firstName.invalid || this.lastName.invalid || this.email.invalid || this.role.invalid);
  }

  close(): void { this.dialog.close(); }

  confirm(): void {
    if (this.invalid()) return;
    this.dialog.close({
      firstName: this.firstName.value.trim(),
      lastName: this.lastName.value.trim(),
      email: this.email.value.trim(),
      role: this.role.value,
      password: this.password.value,
    });
  }
}

export interface UserDialogData {
  kind: 'create' | 'reset';
  title: string;
  help: string;
}

export interface UserDialogResult {
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  password: string;
}
