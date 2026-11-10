import { Component, input } from '@angular/core';
import { actionLabel, formatWhen } from '../../core/labels';
import { AuditEntry } from '../../core/models';

@Component({
  selector: 'app-activity-list',
  template: `
    <section class="card">
      <h2>Actividad reciente</h2>
      @if (entries().length === 0) {
        <p class="empty">Todavía no hay actividad.</p>
      } @else {
        <table class="data">
          <thead><tr><th>Cuándo</th><th>Qué ocurrió</th></tr></thead>
          <tbody>
            @for (entry of entries(); track entry.id) {
              <tr>
                <td>{{ formatWhen(entry.occurredAt) }}</td>
                <td>{{ actionLabel(entry.action) }}</td>
              </tr>
            }
          </tbody>
        </table>
      }
    </section>
  `,
})
export class ActivityList {
  readonly entries = input<AuditEntry[]>([]);
  readonly formatWhen = formatWhen;
  readonly actionLabel = actionLabel;
}
