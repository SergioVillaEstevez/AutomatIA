import { Component, effect, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { ApiService } from '../../core/api.service';
import { ROLE_LABEL, formatWhen } from '../../core/labels';
import { ManagedUser } from '../../core/models';
import { problemMessage } from '../../core/problem';
import { UserDialog } from '../../shared/dialogs';

@Component({
  selector: 'app-user-manager',
  imports: [MatButtonModule],
  templateUrl: './user-manager.html',
})
export class UserManager {
  private readonly api = inject(ApiService);
  private readonly dialog = inject(MatDialog);
  readonly companyId = input<string | null>(null);
  readonly mode = input<'admin' | 'company'>('company');
  readonly users = signal<ManagedUser[]>([]);
  readonly error = signal('');
  readonly roleLabel = ROLE_LABEL;
  readonly formatWhen = formatWhen;

  constructor() {
    effect(() => {
      this.companyId();
      this.mode();
      this.load();
    });
  }

  load(): void {
    const companyId = this.mode() === 'admin' ? this.companyId() : null;
    if (this.mode() === 'admin' && !companyId) {
      return;
    }
    this.api.users(companyId).subscribe({
      next: (users) => this.users.set(users),
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }

  create(): void {
    this.dialog.open(UserDialog, {
      data: {
        kind: 'create',
        title: 'Nuevo usuario',
        help: 'La clave temporal no se envía por correo. Entrégala por un canal seguro. La persona tendrá que cambiarla al entrar.',
      },
    }).afterClosed().subscribe((result) => {
      if (!result) return;
      const companyId = this.mode() === 'admin' ? this.companyId() : null;
      this.api.createUser(companyId, result).subscribe({
        next: () => this.load(),
        error: (error: unknown) => this.error.set(problemMessage(error)),
      });
    });
  }

  reset(user: ManagedUser): void {
    this.dialog.open(UserDialog, {
      data: {
        kind: 'reset',
        title: 'Nueva clave temporal',
        help: `La sesión actual de ${user.email} quedará cerrada y deberá cambiar la clave al volver a entrar.`,
      },
    }).afterClosed().subscribe((result) => {
      if (!result) return;
      const companyId = this.mode() === 'admin' ? this.companyId() : null;
      this.api.resetPassword(companyId, user.id, result.password).subscribe({
        next: () => this.load(),
        error: (error: unknown) => this.error.set(problemMessage(error)),
      });
    });
  }

  toggle(user: ManagedUser): void {
    const enabled = user.status !== 'ACTIVE';
    const companyId = this.mode() === 'admin' ? this.companyId() : null;
    this.api.setUserEnabled(companyId, user.id, enabled).subscribe({
      next: () => this.load(),
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }
}
