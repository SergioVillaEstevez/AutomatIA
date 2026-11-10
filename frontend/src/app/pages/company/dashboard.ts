import { Component, inject, signal } from '@angular/core';
import { ApiService } from '../../core/api.service';
import { CompanyDashboard } from '../../core/models';
import { problemMessage } from '../../core/problem';
import { ActivityList } from '../shared/activity-list';

@Component({
  selector: 'app-company-dashboard',
  imports: [ActivityList],
  templateUrl: './dashboard.html',
})
export class CompanyDashboardPage {
  private readonly api = inject(ApiService);
  readonly data = signal<CompanyDashboard | null>(null);
  readonly error = signal('');

  constructor() {
    this.api.companyDashboard().subscribe({
      next: (data) => this.data.set(data),
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }
}
