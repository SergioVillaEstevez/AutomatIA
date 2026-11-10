import { HttpErrorResponse } from '@angular/common/http';
import { problemMessage } from './problem';

describe('problemMessage', () => {
  it('usa el detalle que devuelve la API', () => {
    const error = new HttpErrorResponse({
      status: 403,
      error: { detail: 'La empresa está suspendida y no puede usar la plataforma.', code: 'COMPANY_SUSPENDED' },
    });
    expect(problemMessage(error)).toContain('suspendida');
  });

  it('no inventa un mensaje técnico si la respuesta no es conocida', () => {
    expect(problemMessage(new Error('socket'))).toBe('No se ha podido completar la operación.');
  });
});
