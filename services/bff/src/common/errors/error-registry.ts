export const ERROR_MESSAGES: Record<string, string> = {
  SESSION_ALREADY_VERIFIED: 'This session has already been verified.',
  CLAIM_ALREADY_EXISTS: 'A claim already exists for this session.',
  POLICY_EXPIRED: 'The policy has expired and cannot be used for new claims.',
  PET_NOT_FOUND: 'Pet not found.',
  SESSION_NOT_FOUND: 'Session not found.',
  CLAIM_NOT_FOUND: 'Claim not found.',
  UNKNOWN_ERROR: 'An unexpected error occurred.',
};

export function fallbackForStatus(status: number): string {
  if (status >= 500) return 'An unexpected server error occurred.';
  if (status === 404) return 'Resource not found.';
  if (status === 422) return 'Validation failed.';
  if (status === 409) return 'Conflict: resource already exists.';
  return 'An unexpected error occurred.';
}
