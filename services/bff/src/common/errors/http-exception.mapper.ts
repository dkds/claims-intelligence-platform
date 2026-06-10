import { HttpException } from '@nestjs/common';
import { AxiosError } from 'axios';
import { ERROR_MESSAGES, fallbackForStatus } from './error-registry.js';

export function toHttpException(err: AxiosError): HttpException {
  const status = err.response?.status ?? 500;
  const body = err.response?.data as Record<string, unknown> | undefined;
  const code = (body?.['code'] ?? body?.['error'] ?? 'UNKNOWN_ERROR') as string;
  const message = ERROR_MESSAGES[code] ?? fallbackForStatus(status);
  return new HttpException({ code, message, status }, status);
}
