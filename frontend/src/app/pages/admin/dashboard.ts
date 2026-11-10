import { Component, inject, signal } from '@angular/core';
import { ApiService } from '../../core/api.service';
import { actionLabel, formatAiCost, formatWhen } from '../../core/labels';
import { AdminDashboard } from '../../core/models';
import { problemMessage } from '../../core/problem';
import { ActivityList } from '../shared/activity-list';

@Component({
  selector: 'app-admin-dashboard',
  imports: [ActivityList],
  templateUrl: './dashboard.html',
})
export class AdminDashboardPage {
  private readonly api = inject(ApiService);
  readonly data = signal<AdminDashboard | null>(null);
  readonly error = signal('');
  readonly formatWhen = formatWhen;
  readonly formatAiCost = formatAiCost;
  readonly actionLabel = actionLabel;

  constructor() {
    this.api.adminDashboard().subscribe({
      next: (data) => this.data.set(data),
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }
}
