export type Role = 'SUPER_ADMIN' | 'COMPANY_ADMIN' | 'COMPANY_USER';
export type CompanyStatus = 'ACTIVE' | 'SUSPENDED';
export type UserStatus = 'ACTIVE' | 'DISABLED';

export interface ProblemBody {
  detail?: string;
  title?: string;
  code?: string;
  errors?: { field: string; message: string }[];
}

export interface CurrentUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
  companyId?: string | null;
  companyName?: string | null;
  passwordChangeRequired: boolean;
}

export interface AuthResponse {
  accessToken: string;
  accessTokenExpiresAt: string;
  user: CurrentUser;
}

export interface PageResult<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface Company {
  id: string;
  legalName: string;
  tradeName: string;
  taxId: string;
  email: string;
  phone?: string | null;
  addressLine?: string | null;
  city?: string | null;
  province?: string | null;
  postalCode?: string | null;
  country: string;
  status: CompanyStatus;
  suspensionReason?: string | null;
  planLabel: string;
  monthlyAiBudgetEur?: number | null;
  aiBudgetEnforced: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CompanyPayload {
  legalName: string;
  tradeName: string;
  taxId: string;
  email: string;
  phone: string | null;
  addressLine: string | null;
  city: string | null;
  province: string | null;
  postalCode: string | null;
  planLabel: string;
  monthlyAiBudgetEur: number | null;
}

export interface ManagedUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  status: UserStatus;
  role: Role;
  passwordChangeRequired: boolean;
  lastLoginAt?: string | null;
  companyId?: string | null;
}

export interface AuditEntry {
  id: string;
  occurredAt: string;
  actorUserId?: string | null;
  companyId?: string | null;
  action: string;
  resourceType: string;
  resourceId?: string | null;
  ip?: string | null;
  details: Record<string, unknown>;
}

export interface AdminDashboard {
  companies: number;
  activeCompanies: number;
  suspendedCompanies: number;
  users: number;
  activeUsers: number;
  monthlyAiSpendEur: number;
  month: string;
  recentActivity: AuditEntry[];
}

export interface CompanyDashboard {
  company: Company;
  activeUsers: number;
  disabledUsers: number;
  monthlyAiSpendEur: number;
  monthlyAiBudgetEur?: number | null;
  aiBudgetEnforced: boolean;
  month: string;
  recentActivity: AuditEntry[];
}

export type AiUsageStatus = 'SUCCESS' | 'FAILED' | 'BLOCKED';

export interface AiUsageEntry {
  id: string;
  companyId: string;
  userId?: string | null;
  provider: string;
  model: string;
  taskType: string;
  inputTokens: number;
  outputTokens: number;
  estimatedCostUsd?: number | null;
  estimatedCostEur?: number | null;
  status: AiUsageStatus;
  errorCode?: string | null;
  errorMessage?: string | null;
  occurredAt: string;
}

export interface AiUsageSummary {
  month: string;
  spentEur: number;
  budgetEur?: number | null;
  budgetEnforced: boolean;
  remainingEur?: number | null;
}

export interface AiPricing {
  id: string;
  provider: string;
  model: string;
  inputUsdPerMillion: number;
  outputUsdPerMillion: number;
  currency: string;
  effectiveFrom: string;
  effectiveTo?: string | null;
  sourceNote: string;
}

export interface AiCompletionResult {
  content: string;
  provider: string;
  model: string;
  inputTokens: number;
  outputTokens: number;
  estimatedCostUsd: number;
  estimatedCostEur: number;
  usageId: string;
}
