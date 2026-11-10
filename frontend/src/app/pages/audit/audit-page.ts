import { Component, effect, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { ApiService } from '../../core/api.service';
import { actionLabel, formatWhen } from '../../core/labels';
import { AuditEntry } from '../../core/models';
import { problemMessage } from '../../core/problem';

@Component({
  selector: 'app-audit-page',
  imports: [MatButtonModule],
  templateUrl: './audit-page.html',
})
export class AuditPage {
  private readonly api = inject(ApiService);
  readonly scope = input.required<'admin' | 'company'>();
  readonly items = signal<AuditEntry[]>([]);
  readonly page = signal(1);
  readonly totalPages = signal(0);
  readonly error = signal('');
  readonly actionLabel = actionLabel;
  readonly formatWhen = formatWhen;

  constructor() {
    effect(() => {
      this.scope();
      this.load(1);
    });
  }

  load(page: number): void {
    this.api.audit(this.scope(), page).subscribe({
      next: (result) => {
        this.items.set(result.items);
        this.page.set(result.page);
        this.totalPages.set(result.totalPages);
      },
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }
}
