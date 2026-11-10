import { Component, inject, signal } from '@angular/core';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { AuthService } from '../../core/auth.service';
import { ApiService } from '../../core/api.service';
import { Company } from '../../core/models';
import { problemMessage } from '../../core/problem';

@Component({
  selector: 'app-company-profile',
  imports: [FormsModule, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './profile.html',
})
export class CompanyProfilePage {
  private readonly api = inject(ApiService);
  readonly auth = inject(AuthService);
  readonly company = signal<Company | null>(null);
  readonly error = signal('');
  readonly saved = signal(false);
  readonly phone = new FormControl('', { nonNullable: true });
  readonly addressLine = new FormControl('', { nonNullable: true });
  readonly city = new FormControl('', { nonNullable: true });
  readonly province = new FormControl('', { nonNullable: true });
  readonly postalCode = new FormControl('', { nonNullable: true });

  constructor() {
    this.api.currentCompany().subscribe({
      next: (company) => {
        this.company.set(company);
        this.phone.setValue(company.phone ?? '');
        this.addressLine.setValue(company.addressLine ?? '');
        this.city.setValue(company.city ?? '');
        this.province.setValue(company.province ?? '');
        this.postalCode.setValue(company.postalCode ?? '');
      },
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }

  save(): void {
    this.saved.set(false);
    this.api.updateContact({
      phone: empty(this.phone.value),
      addressLine: empty(this.addressLine.value),
      city: empty(this.city.value),
      province: empty(this.province.value),
      postalCode: empty(this.postalCode.value),
    }).subscribe({
      next: (company) => {
        this.company.set(company);
        this.saved.set(true);
      },
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }
}

function empty(value: string): string | null {
  const trimmed = value.trim();
  return trimmed ? trimmed : null;
}
