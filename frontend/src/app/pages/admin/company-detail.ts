import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { formatMoney } from '../../core/labels';
import { Company } from '../../core/models';
import { problemMessage } from '../../core/problem';
import { SuspendDialog } from '../../shared/dialogs';
import { UserManager } from '../shared/user-manager';

@Component({
  selector: 'app-company-detail',
  imports: [RouterLink, MatButtonModule, UserManager],
  templateUrl: './company-detail.html',
})
export class CompanyDetailPage {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly dialog = inject(MatDialog);
  readonly company = signal<Company | null>(null);
  readonly error = signal('');
  readonly formatMoney = formatMoney;

  constructor() { this.reload(); }

  reload(): void {
    const id = this.route.snapshot.paramMap.get('companyId');
    if (!id) return;
    this.api.company(id).subscribe({
      next: (company) => this.company.set(company),
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }

  suspend(): void {
    const company = this.company();
    if (!company) return;
    this.dialog.open(SuspendDialog).afterClosed().subscribe((reason?: string) => {
      if (!reason) return;
      this.api.suspend(company.id, reason).subscribe({
        next: (updated) => this.company.set(updated),
        error: (error: unknown) => this.error.set(problemMessage(error)),
      });
    });
  }

  activate(): void {
    const company = this.company();
    if (!company) return;
    this.api.activate(company.id).subscribe({
      next: (updated) => this.company.set(updated),
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }
}
