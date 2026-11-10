import { Component, inject, signal } from '@angular/core';
import { FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { ApiService } from '../../core/api.service';
import { problemMessage } from '../../core/problem';

@Component({
  selector: 'app-hosp-venue-page',
  imports: [FormsModule, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSlideToggleModule],
  templateUrl: './venue-page.html',
})
export class HospVenuePage {
  private readonly api = inject(ApiService);
  readonly error = signal('');
  readonly info = signal('');
  readonly publicPath = signal('');
  readonly slug = new FormControl('', { nonNullable: true, validators: [Validators.required] });
  readonly note = new FormControl('', { nonNullable: true });
  readonly enabled = new FormControl(false, { nonNullable: true });
  readonly floorWidth = new FormControl('900', { nonNullable: true });
  readonly floorHeight = new FormControl('600', { nonNullable: true });

  constructor() {
    this.api.hospVenue().subscribe({
      next: (venue) => {
        this.slug.setValue(venue.publicSlug);
        this.note.setValue(venue.publicBookingNote ?? '');
        this.enabled.setValue(venue.publicBookingEnabled);
        this.floorWidth.setValue(String(venue.floorWidth));
        this.floorHeight.setValue(String(venue.floorHeight));
        this.publicPath.set(venue.publicPath);
      },
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }

  save(): void {
    this.api.saveHospVenue({
      publicSlug: this.slug.value.trim(),
      publicBookingEnabled: this.enabled.value,
      publicBookingNote: this.note.value.trim() || null,
      floorWidth: Number(this.floorWidth.value),
      floorHeight: Number(this.floorHeight.value),
    }).subscribe({
      next: (venue) => {
        this.publicPath.set(venue.publicPath);
        this.info.set('Guardado. Enlace público: ' + venue.publicPath);
      },
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }
}
