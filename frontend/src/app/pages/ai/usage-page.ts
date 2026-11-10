import { Component, effect, inject, input, signal } from '@angular/core';
import { FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ApiService } from '../../core/api.service';
import { aiStatusLabel, formatAiCost, formatMoney, formatWhen } from '../../core/labels';
import { AiCompletionResult, AiUsageEntry, AiUsageSummary } from '../../core/models';
import { problemMessage } from '../../core/problem';

@Component({
  selector: 'app-ai-usage-page',
  imports: [FormsModule, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  templateUrl: './usage-page.html',
})
export class AiUsagePage {
  private readonly api = inject(ApiService);
  readonly scope = input.required<'admin' | 'company'>();
  readonly summary = signal<AiUsageSummary | null>(null);
  readonly items = signal<AiUsageEntry[]>([]);
  readonly page = signal(1);
  readonly totalPages = signal(0);
  readonly error = signal('');
  readonly prompt = new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(4000)] });
  readonly completing = signal(false);
  readonly completion = signal<AiCompletionResult | null>(null);
  readonly formatWhen = formatWhen;
  readonly formatMoney = formatMoney;
  readonly formatAiCost = formatAiCost;
  readonly aiStatusLabel = aiStatusLabel;

  constructor() {
    effect(() => {
      this.scope();
      this.reload();
    });
  }

  reload(): void {
    this.loadSummary();
    this.load(1);
  }

  loadSummary(): void {
    this.api.aiUsageSummary(this.scope()).subscribe({
      next: (summary) => this.summary.set(summary),
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }

  load(page: number): void {
    this.api.aiUsage(this.scope(), page).subscribe({
      next: (result) => {
        this.items.set(result.items);
        this.page.set(result.page);
        this.totalPages.set(result.totalPages);
      },
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }

  tryCompletion(): void {
    if (this.scope() !== 'company' || this.prompt.invalid) {
      return;
    }
    this.completing.set(true);
    this.completion.set(null);
    this.error.set('');
    this.api.completeAi(this.prompt.value.trim()).subscribe({
      next: (result) => {
        this.completing.set(false);
        this.completion.set(result);
        this.reload();
      },
      error: (error: unknown) => {
        this.completing.set(false);
        this.error.set(problemMessage(error));
        this.reload();
      },
    });
  }
}
