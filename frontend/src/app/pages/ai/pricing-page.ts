import { Component, inject, signal } from '@angular/core';
import { FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ApiService } from '../../core/api.service';
import { formatWhen } from '../../core/labels';
import { AiPricing } from '../../core/models';
import { problemMessage } from '../../core/problem';

@Component({
  selector: 'app-ai-pricing-page',
  imports: [FormsModule, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  templateUrl: './pricing-page.html',
})
export class AiPricingPage {
  private readonly api = inject(ApiService);
  readonly items = signal<AiPricing[]>([]);
  readonly error = signal('');
  readonly submitting = signal(false);
  readonly formatWhen = formatWhen;
  readonly provider = new FormControl('openai', { nonNullable: true, validators: [Validators.required] });
  readonly model = new FormControl('', { nonNullable: true, validators: [Validators.required] });
  readonly inputUsd = new FormControl('', { nonNullable: true, validators: [Validators.required] });
  readonly outputUsd = new FormControl('', { nonNullable: true, validators: [Validators.required] });
  readonly sourceNote = new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(300)] });

  constructor() {
    this.reload();
  }

  reload(): void {
    this.api.aiPricing().subscribe({
      next: (items) => this.items.set(items),
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }

  submit(): void {
    if (this.provider.invalid || this.model.invalid || this.inputUsd.invalid || this.outputUsd.invalid || this.sourceNote.invalid) {
      return;
    }
    const inputUsdPerMillion = Number(this.inputUsd.value.replace(',', '.'));
    const outputUsdPerMillion = Number(this.outputUsd.value.replace(',', '.'));
    if (Number.isNaN(inputUsdPerMillion) || Number.isNaN(outputUsdPerMillion)) {
      this.error.set('Los precios tienen que ser números.');
      return;
    }
    this.submitting.set(true);
    this.error.set('');
    this.api.createAiPricing({
      provider: this.provider.value.trim(),
      model: this.model.value.trim(),
      inputUsdPerMillion,
      outputUsdPerMillion,
      sourceNote: this.sourceNote.value.trim(),
    }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.model.setValue('');
        this.inputUsd.setValue('');
        this.outputUsd.setValue('');
        this.sourceNote.setValue('');
        this.reload();
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        this.error.set(problemMessage(error));
      },
    });
  }
}
