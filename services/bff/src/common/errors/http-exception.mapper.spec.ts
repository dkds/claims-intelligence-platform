import { AxiosError } from 'axios';
import { toHttpException } from './http-exception.mapper.js';

function axiosError(
  status?: number,
  data?: Record<string, unknown>,
): AxiosError {
  return {
    isAxiosError: true,
    name: 'AxiosError',
    message: 'Request failed',
    toJSON: () => ({}),
    response:
      status === undefined
        ? undefined
        : { status, data, statusText: '', headers: {}, config: {} as never },
  } as AxiosError;
}

describe('toHttpException', () => {
  it('maps a known error code to its friendly message', () => {
    const exception = toHttpException(
      axiosError(404, { code: 'CLAIM_NOT_FOUND' }),
    );

    expect(exception.getStatus()).toBe(404);
    expect(exception.getResponse()).toEqual({
      code: 'CLAIM_NOT_FOUND',
      message: 'Claim not found.',
      status: 404,
    });
  });

  it('falls back to a status-based message for an unrecognised error code', () => {
    const exception = toHttpException(
      axiosError(409, { code: 'SOME_UNMAPPED_CODE' }),
    );

    expect(exception.getResponse()).toMatchObject({
      code: 'SOME_UNMAPPED_CODE',
      message: 'Conflict: resource already exists.',
      status: 409,
    });
  });

  it('reads the code from an "error" field when "code" is absent', () => {
    const exception = toHttpException(
      axiosError(422, { error: 'PET_NOT_FOUND' }),
    );

    expect(exception.getResponse()).toMatchObject({
      code: 'PET_NOT_FOUND',
      message: 'Pet not found.',
    });
  });

  it('defaults to a 500 UNKNOWN_ERROR when the response has no data', () => {
    const exception = toHttpException(axiosError(undefined));

    expect(exception.getStatus()).toBe(500);
    expect(exception.getResponse()).toEqual({
      code: 'UNKNOWN_ERROR',
      message: 'An unexpected error occurred.',
      status: 500,
    });
  });
});
