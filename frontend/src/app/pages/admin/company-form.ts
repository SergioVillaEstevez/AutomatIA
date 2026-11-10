import { Component, inject, signal } from '@angular/core';
import { FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { CompanyPayload } from '../../core/models';
import { problemMessage } from '../../core/problem';

@Component({
  selector: 'app-company-form',
  imports: [FormsModule, ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './company-form.html',
})
export class CompanyFormPage {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly companyId = this.route.snapshot.paramMap.get('companyId');
  readonly error = signal('');
  readonly submitting = signal(false);
  readonly legalName = new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(200)] });
  readonly tradeName = new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(200)] });
  readonly taxId = new FormControl('', { nonNullable: true, validators: [Validators.required] });
  readonly email = new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] });
  readonly phone = new FormControl('', { nonNullable: true });
  readonly addressLine = new FormControl('', { nonNullable: true });
  readonly city = new FormControl('', { nonNullable: true });
  readonly province = new FormControl('Toledo', { nonNullable: true });
  readonly postalCode = new FormControl('', { nonNullable: true });
  readonly planLabel = new FormControl('PILOTO', { nonNullable: true, validators: [Validators.required, Validators.maxLength(40)] });
  readonly budget = new FormControl('', { nonNullable: true });

  constructor() {
    if (this.companyId) {
      this.api.company(this.companyId).subscribe({
        next: (company) => {
          this.legalName.setValue(company.legalName);
          this.tradeName.setValue(company.tradeName);
          this.taxId.setValue(company.taxId);
          this.email.setValue(company.email);
          this.phone.setValue(company.phone ?? '');
          this.addressLine.setValue(company.addressLine ?? '');
          this.city.setValue(company.city ?? '');
          this.province.setValue(company.province ?? '');
          this.postalCode.setValue(company.postalCode ?? '');
          this.planLabel.setValue(company.planLabel);
          this.budget.setValue(company.monthlyAiBudgetEur === null || company.monthlyAiBudgetEur === undefined ? '' : String(company.monthlyAiBudgetEur));
        },
        error: (error: unknown) => this.error.set(problemMessage(error)),
      });
    }
  }

  submit(): void {
    if (this.legalName.invalid || this.tradeName.invalid || this.taxId.invalid || this.email.invalid || this.planLabel.invalid) {
      return;
    }
    const budgetText = this.budget.value.trim();
    const payload: CompanyPayload = {
      legalName: this.legalName.value.trim(),
      tradeName: this.tradeName.value.trim(),
      taxId: this.taxId.value.trim(),
      email: this.email.value.trim(),
      phone: emptyToNull(this.phone.value),
      addressLine: emptyToNull(this.addressLine.value),
      city: emptyToNull(this.city.value),
      province: emptyToNull(this.province.value),
      postalCode: emptyToNull(this.postalCode.value),
      planLabel: this.planLabel.value.trim(),
      monthlyAiBudgetEur: budgetText ? Number(budgetText.replace(',', '.')) : null,
    };
    if (payload.monthlyAiBudgetEur !== null && Number.isNaN(payload.monthlyAiBudgetEur)) {
      this.error.set('El límite mensual tiene que ser un importe.');
      return;
    }
    this.submitting.set(true);
    const request = this.companyId
      ? this.api.updateCompany(this.companyId, payload)
      : this.api.createCompany(payload);
    request.subscribe({
      next: (company) => void this.router.navigate(['/admin/empresas', company.id]),
      error: (error: unknown) => {
        this.submitting.set(false);
        this.error.set(problemMessage(error));
      },
    });
  }
}

function emptyToNull(value: string): string | null {
  const trimmed = value.trim();
  return trimmed ? trimmed : null;
}
