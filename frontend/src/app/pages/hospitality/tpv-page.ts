import { Component, inject, signal } from '@angular/core';
import { FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ApiService } from '../../core/api.service';
import { formatAiCost, formatWhen } from '../../core/labels';
import { HospitalityResource, PaymentMethod, Ticket } from '../../core/models';
import { problemMessage } from '../../core/problem';

@Component({
  selector: 'app-hosp-tpv-page',
  imports: [FormsModule, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  templateUrl: './tpv-page.html',
})
export class HospTpvPage {
  private readonly api = inject(ApiService);
  readonly tickets = signal<Ticket[]>([]);
  readonly resources = signal<HospitalityResource[]>([]);
  readonly selected = signal<Ticket | null>(null);
  readonly error = signal('');
  readonly formatWhen = formatWhen;
  readonly formatAiCost = formatAiCost;
  readonly label = new FormControl('Ticket', { nonNullable: true, validators: [Validators.required] });
  readonly resourceId = new FormControl('', { nonNullable: true });
  readonly description = new FormControl('', { nonNullable: true, validators: [Validators.required] });
  readonly quantity = new FormControl('1', { nonNullable: true });
  readonly unitPrice = new FormControl('10', { nonNullable: true });

  constructor() {
    this.reload();
    this.api.hospResources().subscribe({
      next: (items) => this.resources.set(items.filter((item) => item.active)),
    });
  }

  reload(): void {
    this.api.hospTickets().subscribe({
      next: (items) => this.tickets.set(items),
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }

  openTicket(): void {
    this.api.createHospTicket({
      label: this.label.value.trim(),
      resourceId: this.resourceId.value || null,
    }).subscribe({
      next: (ticket) => {
        this.selected.set(ticket);
        this.reload();
      },
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }

  select(ticket: Ticket): void {
    this.selected.set(ticket);
  }

  addLine(): void {
    const ticket = this.selected();
    if (!ticket || ticket.status !== 'OPEN' || this.description.invalid) {
      return;
    }
    const quantity = Number(this.quantity.value.replace(',', '.'));
    const unitPriceEur = Number(this.unitPrice.value.replace(',', '.'));
    this.api.addHospTicketLine(ticket.id, {
      description: this.description.value.trim(),
      quantity,
      unitPriceEur,
    }).subscribe({
      next: (updated) => {
        this.selected.set(updated);
        this.description.setValue('');
        this.reload();
      },
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }

  pay(method: PaymentMethod): void {
    const ticket = this.selected();
    if (!ticket) return;
    this.api.payHospTicket(ticket.id, method).subscribe({
      next: (updated) => {
        this.selected.set(updated);
        this.reload();
      },
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }

  cancel(): void {
    const ticket = this.selected();
    if (!ticket) return;
    this.api.cancelHospTicket(ticket.id).subscribe({
      next: (updated) => {
        this.selected.set(updated);
        this.reload();
      },
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }
}
