import { HttpErrorResponse } from '@angular/common/http';
import { ProblemBody } from './models';

export function problemMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const body = error.error as ProblemBody | null;
    if (body?.errors?.length) {
      return body.errors.map((item) => item.message).join(' ');
    }
    if (body?.detail) {
      return body.detail;
    }
  }
  return 'No se ha podido completar la operación.';
}
