import { Component, inject, signal } from '@angular/core';
import { FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ApiService } from '../../core/api.service';
import { WhatsAppTemplate } from '../../core/models';
import { problemMessage } from '../../core/problem';

@Component({
  selector: 'app-hosp-templates-page',
  imports: [FormsModule, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  templateUrl: './templates-page.html',
})
export class HospTemplatesPage {
  private readonly api = inject(ApiService);
  readonly items = signal<WhatsAppTemplate[]>([]);
  readonly error = signal('');
  readonly code = new FormControl('', { nonNullable: true, validators: [Validators.required] });
  readonly title = new FormControl('', { nonNullable: true, validators: [Validators.required] });
  readonly body = new FormControl(
    'Hola {{guestName}}, tu reserva el {{startsAt}} para {{partySize}} personas está confirmada.',
    { nonNullable: true, validators: [Validators.required] },
  );
  readonly meta = new FormControl('', { nonNullable: true });

  constructor() {
    this.reload();
  }

  reload(): void {
    this.api.hospWhatsAppTemplates().subscribe({
      next: (items) => this.items.set(items),
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }

  create(): void {
    if (this.code.invalid || this.title.invalid || this.body.invalid) {
      return;
    }
    this.api.createHospWhatsAppTemplate({
      code: this.code.value.trim(),
      title: this.title.value.trim(),
      body: this.body.value.trim(),
      metaTemplateName: this.meta.value.trim() || null,
    }).subscribe({
      next: () => {
        this.code.setValue('');
        this.title.setValue('');
        this.reload();
      },
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }

  toggle(item: WhatsAppTemplate): void {
    this.api.updateHospWhatsAppTemplate(item.id, {
      code: item.code,
      title: item.title,
      body: item.body,
      metaTemplateName: item.metaTemplateName,
      active: !item.active,
    }).subscribe({
      next: () => this.reload(),
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }
}
