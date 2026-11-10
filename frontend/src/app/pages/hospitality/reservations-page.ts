import { Component, inject, signal } from '@angular/core';
import { FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ApiService } from '../../core/api.service';
import { formatWhen, hospStatusLabel } from '../../core/labels';
import {
  HospitalityOccupancy,
  HospitalityReservation,
  HospitalityResource,
  ReservationStatus,
} from '../../core/models';
import { problemMessage } from '../../core/problem';

@Component({
  selector: 'app-hosp-reservations-page',
  imports: [
    FormsModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './reservations-page.html',
})
export class HospReservationsPage {
  private readonly api = inject(ApiService);
  readonly day = signal(localDay());
  readonly occupancy = signal<HospitalityOccupancy | null>(null);
  readonly items = signal<HospitalityReservation[]>([]);
  readonly resources = signal<HospitalityResource[]>([]);
  readonly whatsappConfigured = signal(false);
  readonly whatsappDetail = signal('');
  readonly error = signal('');
  readonly info = signal('');
  readonly submitting = signal(false);
  readonly formatWhen = formatWhen;
  readonly hospStatusLabel = hospStatusLabel;

  readonly guestName = new FormControl('', { nonNullable: true, validators: [Validators.required] });
  readonly guestPhone = new FormControl('', { nonNullable: true });
  readonly partySize = new FormControl('2', { nonNullable: true, validators: [Validators.required] });
  readonly resourceId = new FormControl('', { nonNullable: true });
  readonly time = new FormControl('21:00', { nonNullable: true, validators: [Validators.required] });
  readonly notes = new FormControl('', { nonNullable: true });

  constructor() {
    this.reload();
    this.api.hospResources().subscribe({
      next: (items) => this.resources.set(items.filter((item) => item.active)),
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
    this.api.hospWhatsAppStatus().subscribe({
      next: (status) => {
        this.whatsappConfigured.set(status.configured);
        this.whatsappDetail.set(status.detail);
      },
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }

  changeDay(value: string): void {
    if (!value) {
      return;
    }
    this.day.set(value);
    this.reload();
  }

  reload(): void {
    const day = this.day();
    this.api.hospOccupancy(day).subscribe({
      next: (occupancy) => this.occupancy.set(occupancy),
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
    this.api.hospReservations(day, 1).subscribe({
      next: (page) => this.items.set(page.items),
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }

  create(): void {
    if (this.guestName.invalid || this.partySize.invalid || this.time.invalid) {
      return;
    }
    const partySize = Number(this.partySize.value);
    if (!Number.isInteger(partySize) || partySize < 1) {
      this.error.set('El número de comensales no es válido.');
      return;
    }
    const startsAt = localDateTimeToIso(this.day(), this.time.value);
    this.submitting.set(true);
    this.error.set('');
    this.info.set('');
    this.api.createHospReservation({
      resourceId: this.resourceId.value || null,
      guestName: this.guestName.value.trim(),
      guestPhone: this.guestPhone.value.trim() || null,
      partySize,
      startsAt,
      status: 'CONFIRMED',
      notes: this.notes.value.trim() || null,
    }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.guestName.setValue('');
        this.guestPhone.setValue('');
        this.notes.setValue('');
        this.reload();
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        this.error.set(problemMessage(error));
      },
    });
  }

  setStatus(item: HospitalityReservation, status: ReservationStatus): void {
    this.api.updateHospReservation(item.id, {
      resourceId: item.resourceId,
      guestName: item.guestName,
      guestPhone: item.guestPhone,
      partySize: item.partySize,
      startsAt: item.startsAt,
      endsAt: item.endsAt,
      status,
      notes: item.notes,
    }).subscribe({
      next: () => this.reload(),
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }

  notify(item: HospitalityReservation): void {
    this.info.set('');
    this.error.set('');
    this.api.notifyHospReservation(item.id).subscribe({
      next: (message) => {
        this.info.set(`WhatsApp enviado (${message.status}).`);
        this.reload();
      },
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }

  occupancyPercent(): string {
    const occupancy = this.occupancy();
    if (!occupancy) {
      return '—';
    }
    return `${Math.round(occupancy.occupancyRatio * 100)} %`;
  }
}

function localDay(): string {
  const now = new Date();
  const madrid = new Date(now.toLocaleString('en-US', { timeZone: 'Europe/Madrid' }));
  const y = madrid.getFullYear();
  const m = String(madrid.getMonth() + 1).padStart(2, '0');
  const d = String(madrid.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

function localDateTimeToIso(day: string, time: string): string {
  const [year, month, date] = day.split('-').map(Number);
  const [hour, minute] = time.split(':').map(Number);
  let guess = Date.UTC(year, month - 1, date, hour, minute, 0);
  for (let i = 0; i < 3; i++) {
    const parts = new Intl.DateTimeFormat('en-CA', {
      timeZone: 'Europe/Madrid',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false,
    }).formatToParts(new Date(guess));
    const value = (type: string) => Number(parts.find((part) => part.type === type)?.value ?? '0');
    let localHour = value('hour');
    if (localHour === 24) {
      localHour = 0;
    }
    const asUtc = Date.UTC(value('year'), value('month') - 1, value('day'), localHour, value('minute'), value('second'));
    const desired = Date.UTC(year, month - 1, date, hour, minute, 0);
    guess += desired - asUtc;
  }
  return new Date(guess).toISOString();
}
