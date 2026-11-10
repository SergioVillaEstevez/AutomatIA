import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AdminDashboard,
  AiCompletionResult,
  AiPricing,
  AiUsageEntry,
  AiUsageSummary,
  AuditEntry,
  Company,
  CompanyDashboard,
  CompanyPayload,
  HospitalityOccupancy,
  HospitalityReservation,
  HospitalityResource,
  ManagedUser,
  PageResult,
  ReservationStatus,
  WhatsAppMessageResult,
  WhatsAppStatus,
} from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  adminDashboard(): Observable<AdminDashboard> {
    return this.http.get<AdminDashboard>('/api/v1/admin/dashboard');
  }

  companyDashboard(): Observable<CompanyDashboard> {
    return this.http.get<CompanyDashboard>('/api/v1/company/dashboard');
  }

  companies(query: string, page: number): Observable<PageResult<Company>> {
    let params = new HttpParams().set('page', page).set('size', 20);
    if (query.trim()) {
      params = params.set('q', query.trim());
    }
    return this.http.get<PageResult<Company>>('/api/v1/admin/companies', { params });
  }

  company(id: string): Observable<Company> {
    return this.http.get<Company>(`/api/v1/admin/companies/${id}`);
  }

  currentCompany(): Observable<Company> {
    return this.http.get<Company>('/api/v1/company');
  }

  createCompany(payload: CompanyPayload): Observable<Company> {
    return this.http.post<Company>('/api/v1/admin/companies', payload);
  }

  updateCompany(id: string, payload: CompanyPayload): Observable<Company> {
    return this.http.put<Company>(`/api/v1/admin/companies/${id}`, payload);
  }

  updateContact(payload: Pick<CompanyPayload, 'phone' | 'addressLine' | 'city' | 'province' | 'postalCode'>): Observable<Company> {
    return this.http.put<Company>('/api/v1/company', payload);
  }

  suspend(id: string, reason: string): Observable<Company> {
    return this.http.post<Company>(`/api/v1/admin/companies/${id}/suspension`, { reason });
  }

  activate(id: string): Observable<Company> {
    return this.http.post<Company>(`/api/v1/admin/companies/${id}/activation`, {});
  }

  users(companyId: string | null): Observable<ManagedUser[]> {
    const url = companyId ? `/api/v1/admin/companies/${companyId}/users` : '/api/v1/company/users';
    return this.http.get<ManagedUser[]>(url);
  }

  createUser(companyId: string | null, body: object): Observable<ManagedUser> {
    const url = companyId ? `/api/v1/admin/companies/${companyId}/users` : '/api/v1/company/users';
    return this.http.post<ManagedUser>(url, body);
  }

  setUserEnabled(companyId: string | null, userId: string, enabled: boolean): Observable<ManagedUser> {
    const base = companyId ? `/api/v1/admin/companies/${companyId}/users` : '/api/v1/company/users';
    return this.http.post<ManagedUser>(`${base}/${userId}/${enabled ? 'activation' : 'deactivation'}`, {});
  }

  resetPassword(companyId: string | null, userId: string, password: string): Observable<ManagedUser> {
    const base = companyId ? `/api/v1/admin/companies/${companyId}/users` : '/api/v1/company/users';
    return this.http.post<ManagedUser>(`${base}/${userId}/password`, { password });
  }

  audit(scope: 'admin' | 'company', page: number): Observable<PageResult<AuditEntry>> {
    const params = new HttpParams().set('page', page).set('size', 20);
    const url = scope === 'admin' ? '/api/v1/admin/audit' : '/api/v1/company/audit';
    return this.http.get<PageResult<AuditEntry>>(url, { params });
  }

  aiUsage(scope: 'admin' | 'company', page: number): Observable<PageResult<AiUsageEntry>> {
    const params = new HttpParams().set('page', page).set('size', 20);
    const url = scope === 'admin' ? '/api/v1/admin/ai/usage' : '/api/v1/company/ai/usage';
    return this.http.get<PageResult<AiUsageEntry>>(url, { params });
  }

  aiUsageSummary(scope: 'admin' | 'company'): Observable<AiUsageSummary> {
    const url = scope === 'admin' ? '/api/v1/admin/ai/usage/summary' : '/api/v1/company/ai/usage/summary';
    return this.http.get<AiUsageSummary>(url);
  }

  aiPricing(): Observable<AiPricing[]> {
    return this.http.get<AiPricing[]>('/api/v1/admin/ai/pricing');
  }

  createAiPricing(body: {
    provider: string;
    model: string;
    inputUsdPerMillion: number;
    outputUsdPerMillion: number;
    sourceNote: string;
    effectiveFrom?: string | null;
  }): Observable<AiPricing> {
    return this.http.post<AiPricing>('/api/v1/admin/ai/pricing', body);
  }

  completeAi(prompt: string, taskType = 'DEFAULT'): Observable<AiCompletionResult> {
    return this.http.post<AiCompletionResult>('/api/v1/company/ai/completions', { prompt, taskType });
  }

  hospResources(): Observable<HospitalityResource[]> {
    return this.http.get<HospitalityResource[]>('/api/v1/company/hospitality/resources');
  }

  createHospResource(body: { name: string; seats: number; sortOrder?: number; active?: boolean }): Observable<HospitalityResource> {
    return this.http.post<HospitalityResource>('/api/v1/company/hospitality/resources', body);
  }

  updateHospResource(
    id: string,
    body: { name: string; seats: number; sortOrder?: number; active?: boolean },
  ): Observable<HospitalityResource> {
    return this.http.put<HospitalityResource>(`/api/v1/company/hospitality/resources/${id}`, body);
  }

  hospReservations(day: string | null, page: number): Observable<PageResult<HospitalityReservation>> {
    let params = new HttpParams().set('page', page).set('size', 50);
    if (day) {
      params = params.set('day', day);
    }
    return this.http.get<PageResult<HospitalityReservation>>('/api/v1/company/hospitality/reservations', { params });
  }

  createHospReservation(body: {
    resourceId?: string | null;
    guestName: string;
    guestPhone?: string | null;
    partySize: number;
    startsAt: string;
    endsAt?: string | null;
    status?: ReservationStatus;
    notes?: string | null;
  }): Observable<HospitalityReservation> {
    return this.http.post<HospitalityReservation>('/api/v1/company/hospitality/reservations', body);
  }

  updateHospReservation(
    id: string,
    body: {
      resourceId?: string | null;
      guestName: string;
      guestPhone?: string | null;
      partySize: number;
      startsAt: string;
      endsAt: string;
      status: ReservationStatus;
      notes?: string | null;
    },
  ): Observable<HospitalityReservation> {
    return this.http.put<HospitalityReservation>(`/api/v1/company/hospitality/reservations/${id}`, body);
  }

  hospOccupancy(day?: string | null): Observable<HospitalityOccupancy> {
    let params = new HttpParams();
    if (day) {
      params = params.set('day', day);
    }
    return this.http.get<HospitalityOccupancy>('/api/v1/company/hospitality/occupancy', { params });
  }

  hospWhatsAppStatus(): Observable<WhatsAppStatus> {
    return this.http.get<WhatsAppStatus>('/api/v1/company/hospitality/whatsapp/status');
  }

  notifyHospReservation(id: string): Observable<WhatsAppMessageResult> {
    return this.http.post<WhatsAppMessageResult>(`/api/v1/company/hospitality/reservations/${id}/whatsapp`, {});
  }
}
