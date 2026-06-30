import { HttpErrorResponse } from '@angular/common/http';
import { ApiResponse } from '../models/api-response.model';

export function extractApiErrorMessage(
  error: unknown,
  fallback = 'Ocurrió un error inesperado. Intente de nuevo.',
): string {
  if (!(error instanceof HttpErrorResponse)) {
    return fallback;
  }

  const body = error.error as ApiResponse<unknown> | Record<string, string> | null;

  if (body && typeof body === 'object' && 'message' in body && typeof body.message === 'string') {
    const data = body.data;
    if (data && typeof data === 'object' && !Array.isArray(data)) {
      const fieldMessages = Object.values(data as Record<string, string>).filter(Boolean);
      if (fieldMessages.length > 0) {
        return fieldMessages.join('. ');
      }
    }
    return body.message;
  }

  if (error.status === 0) {
    return 'No se pudo conectar con el servidor. Verifique su conexión.';
  }

  return fallback;
}
