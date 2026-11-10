import { Role } from './models';

export const ROLE_LABEL: Record<Role, string> = {
  SUPER_ADMIN: 'Administrador general',
  COMPANY_ADMIN: 'Administrador de empresa',
  COMPANY_USER: 'Usuario',
};

export const ACTION_LABEL: Record<string, string> = {
  AUTH_LOGIN_SUCCESS: 'Inicio de sesión',
  AUTH_LOGIN_FAILURE: 'Intento de acceso fallido',
  AUTH_LOGOUT: 'Cierre de sesión',
  AUTH_REFRESH: 'Renovación de sesión',
  AUTH_REFRESH_REUSE: 'Reutilización de una sesión revocada',
  AUTH_PASSWORD_CHANGED: 'Cambio de clave',
  COMPANY_CREATED: 'Alta de empresa',
  COMPANY_UPDATED: 'Modificación de empresa',
  COMPANY_SUSPENDED: 'Suspensión de empresa',
  COMPANY_ACTIVATED: 'Reactivación de empresa',
  USER_CREATED: 'Alta de usuario',
  USER_UPDATED: 'Modificación de usuario',
  USER_DISABLED: 'Usuario desactivado',
  USER_ENABLED: 'Usuario activado',
  USER_PASSWORD_RESET: 'Clave temporal restablecida',
  HOSP_RESOURCE_CREATED: 'Alta de mesa',
  HOSP_RESOURCE_UPDATED: 'Cambio de mesa',
  HOSP_RESERVATION_CREATED: 'Alta de reserva',
  HOSP_RESERVATION_UPDATED: 'Cambio de reserva',
  HOSP_WHATSAPP_SENT: 'Aviso WhatsApp de reserva',
};

export function actionLabel(action: string): string {
  return ACTION_LABEL[action] ?? action;
}

export function formatWhen(value?: string | null): string {
  if (!value) {
    return 'Sin registro';
  }
  return new Intl.DateTimeFormat('es-ES', {
    dateStyle: 'short',
    timeStyle: 'short',
    timeZone: 'Europe/Madrid',
  }).format(new Date(value));
}

export function formatMoney(value?: number | null): string {
  if (value === null || value === undefined) {
    return 'Sin límite';
  }
  return new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' }).format(value);
}

export function formatAiCost(value?: number | null): string {
  if (value === null || value === undefined) {
    return '—';
  }
  return new Intl.NumberFormat('es-ES', {
    style: 'currency',
    currency: 'EUR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 6,
  }).format(value);
}

export const AI_STATUS_LABEL: Record<string, string> = {
  SUCCESS: 'Correcta',
  FAILED: 'Fallida',
  BLOCKED: 'Bloqueada',
};

export function aiStatusLabel(status: string): string {
  return AI_STATUS_LABEL[status] ?? status;
}

export const HOSP_STATUS_LABEL: Record<string, string> = {
  PENDING: 'Pendiente',
  CONFIRMED: 'Confirmada',
  SEATED: 'Sentada',
  COMPLETED: 'Cerrada',
  CANCELLED: 'Cancelada',
  NO_SHOW: 'No presentado',
};

export function hospStatusLabel(status: string): string {
  return HOSP_STATUS_LABEL[status] ?? status;
}
