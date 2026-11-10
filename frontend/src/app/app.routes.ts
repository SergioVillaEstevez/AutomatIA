import { Routes } from '@angular/router';
import { authGuard, guestGuard, roleGuard } from './core/auth.guard';
import { Shell } from './layout/shell';
import { HomeRedirect } from './pages/home-redirect';
import { LoginPage } from './pages/login/login';
import { ChangePasswordPage } from './pages/change-password/change-password';
import { AdminDashboardPage } from './pages/admin/dashboard';
import { CompaniesPage } from './pages/admin/companies';
import { CompanyFormPage } from './pages/admin/company-form';
import { CompanyDetailPage } from './pages/admin/company-detail';
import { AuditPage } from './pages/audit/audit-page';
import { CompanyDashboardPage } from './pages/company/dashboard';
import { CompanyProfilePage } from './pages/company/profile';
import { UserManager } from './pages/shared/user-manager';
import { AiUsagePage } from './pages/ai/usage-page';
import { AiPricingPage } from './pages/ai/pricing-page';

export const routes: Routes = [
  { path: 'login', component: LoginPage, canActivate: [guestGuard] },
  { path: 'cambiar-clave', component: ChangePasswordPage, canActivate: [authGuard] },
  {
    path: '',
    component: Shell,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', component: HomeRedirect },
      {
        path: 'admin',
        canActivate: [roleGuard('SUPER_ADMIN')],
        children: [
          { path: '', component: AdminDashboardPage },
          { path: 'empresas', component: CompaniesPage },
          { path: 'empresas/nueva', component: CompanyFormPage },
          { path: 'empresas/:companyId', component: CompanyDetailPage },
          { path: 'empresas/:companyId/editar', component: CompanyFormPage },
          { path: 'ia', component: AiUsagePage, data: { scope: 'admin' } },
          { path: 'tarifas-ia', component: AiPricingPage },
          { path: 'actividad', component: AuditPage, data: { scope: 'admin' } },
        ],
      },
      {
        path: 'empresa',
        canActivate: [roleGuard('COMPANY_ADMIN', 'COMPANY_USER')],
        children: [
          { path: '', component: CompanyDashboardPage },
          { path: 'usuarios', component: UserManager, canActivate: [roleGuard('COMPANY_ADMIN')] },
          { path: 'datos', component: CompanyProfilePage },
          { path: 'ia', component: AiUsagePage, data: { scope: 'company' } },
          { path: 'actividad', component: AuditPage, data: { scope: 'company' } },
        ],
      },
    ],
  },
];
