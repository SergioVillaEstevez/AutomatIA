import { Component, inject, signal } from '@angular/core';
import { FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ApiService } from '../../core/api.service';
import { HospitalityResource } from '../../core/models';
import { problemMessage } from '../../core/problem';

@Component({
  selector: 'app-hosp-resources-page',
  imports: [FormsModule, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  templateUrl: './resources-page.html',
})
export class HospResourcesPage {
  private readonly api = inject(ApiService);
  readonly items = signal<HospitalityResource[]>([]);
  readonly error = signal('');
  readonly submitting = signal(false);
  readonly name = new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(80)] });
  readonly seats = new FormControl('4', { nonNullable: true, validators: [Validators.required] });

  constructor() {
    this.reload();
  }

  reload(): void {
    this.api.hospResources().subscribe({
      next: (items) => this.items.set(items),
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }

  create(): void {
    if (this.name.invalid || this.seats.invalid) {
      return;
    }
    const seats = Number(this.seats.value);
    if (!Number.isInteger(seats) || seats < 1) {
      this.error.set('Las plazas tienen que ser un entero positivo.');
      return;
    }
    this.submitting.set(true);
    this.error.set('');
    this.api.createHospResource({ name: this.name.value.trim(), seats, sortOrder: this.items().length + 1 }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.name.setValue('');
        this.reload();
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        this.error.set(problemMessage(error));
      },
    });
  }

  toggle(resource: HospitalityResource): void {
    this.api.updateHospResource(resource.id, {
      name: resource.name,
      seats: resource.seats,
      sortOrder: resource.sortOrder,
      active: !resource.active,
      posX: resource.posX,
      posY: resource.posY,
      width: resource.width,
      height: resource.height,
    }).subscribe({
      next: () => this.reload(),
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }
}
