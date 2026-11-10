import { Component, effect, inject, input, signal } from '@angular/core';
import { FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ApiService } from '../../core/api.service';
import { PublicVenue } from '../../core/models';
import { problemMessage } from '../../core/problem';

@Component({
  selector: 'app-public-booking-page',
  imports: [FormsModule, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  templateUrl: './public-booking-page.html',
})
export class PublicBookingPage {
  private readonly api = inject(ApiService);
  readonly slug = input.required<string>();
  readonly venue = signal<PublicVenue | null>(null);
  readonly error = signal('');
  readonly done = signal('');
  readonly guestName = new FormControl('', { nonNullable: true, validators: [Validators.required] });
  readonly guestPhone = new FormControl('', { nonNullable: true });
  readonly partySize = new FormControl('2', { nonNullable: true, validators: [Validators.required] });
  readonly resourceId = new FormControl('', { nonNullable: true });
  readonly day = new FormControl(localDay(), { nonNullable: true, validators: [Validators.required] });
  readonly time = new FormControl('21:00', { nonNullable: true, validators: [Validators.required] });
  readonly notes = new FormControl('', { nonNullable: true });

  constructor() {
    effect(() => {
      const slug = this.slug();
      if (!slug) {
        return;
      }
      this.api.publicVenue(slug).subscribe({
        next: (venue) => this.venue.set(venue),
        error: (error: unknown) => this.error.set(problemMessage(error)),
      });
    });
  }

  submit(): void {
    if (this.guestName.invalid || this.partySize.invalid || this.day.invalid || this.time.invalid) {
      return;
    }
    const partySize = Number(this.partySize.value);
    this.api.publicBook(this.slug(), {
      resourceId: this.resourceId.value || null,
      guestName: this.guestName.value.trim(),
      guestPhone: this.guestPhone.value.trim() || null,
      partySize,
      startsAt: localDateTimeToIso(this.day.value, this.time.value),
      notes: this.notes.value.trim() || null,
    }).subscribe({
      next: (result) => {
        this.done.set(result.detail);
        this.error.set('');
      },
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }
}

function localDay(): string {
  const madrid = new Date(new Date().toLocaleString('en-US', { timeZone: 'Europe/Madrid' }));
  return `${madrid.getFullYear()}-${String(madrid.getMonth() + 1).padStart(2, '0')}-${String(madrid.getDate()).padStart(2, '0')}`;
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
    if (localHour === 24) localHour = 0;
    const asUtc = Date.UTC(value('year'), value('month') - 1, value('day'), localHour, value('minute'), value('second'));
    const desired = Date.UTC(year, month - 1, date, hour, minute, 0);
    guess += desired - asUtc;
  }
  return new Date(guess).toISOString();
}
