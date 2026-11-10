import { Component, inject, signal } from '@angular/core';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { formatMoney } from '../../core/labels';
import { Company } from '../../core/models';
import { problemMessage } from '../../core/problem';

@Component({
  selector: 'app-companies',
  imports: [FormsModule, ReactiveFormsModule, RouterLink, MatButtonModule, MatFormFieldModule, MatInputModule],
  templateUrl: './companies.html',
})
export class CompaniesPage {
  private readonly api = inject(ApiService);
  readonly query = new FormControl('', { nonNullable: true });
  readonly items = signal<Company[]>([]);
  readonly page = signal(1);
  readonly totalPages = signal(0);
  readonly error = signal('');
  readonly formatMoney = formatMoney;

  constructor() { this.load(1); }

  search(): void { this.load(1); }

  load(page: number): void {
    this.api.companies(this.query.value, page).subscribe({
      next: (result) => {
        this.items.set(result.items);
        this.page.set(result.page);
        this.totalPages.set(result.totalPages);
        this.error.set('');
      },
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }
}
