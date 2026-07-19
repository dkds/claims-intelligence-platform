import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { renderWithQueryClient } from '../test/render-utils';
import { ClaimDetail } from './ClaimDetail';
import * as claimsApi from '../api/claims';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', () => ({
  useParams: () => ({ id: 'claim-1' }),
  useNavigate: () => mockNavigate,
}));

vi.mock('../auth/useAuth', () => ({
  useAuth: () => ({
    user: {
      id: 'adj-1',
      email: 'adjuster@clinic.com',
      role: 'adjuster',
      name: 'Sam Adjuster',
      clinicId: 'clinic-1',
    },
  }),
}));

vi.mock('../api/claims');

const baseClaim = {
  _id: 'claim-1',
  clinicId: 'clinic-1',
  petId: 'pet-1',
  policyId: 'policy-1',
  origin: 'manual',
  status: 'PENDING_REVIEW',
  submittedBy: 'mgr-1',
  totalRequested: 120,
  lines: [{ procedureCode: 'DENT-CLEAN', quantity: 1, requestedAmount: 120 }],
  assembledAt: '2026-06-01T09:00:00Z',
  updatedAt: '2026-06-01T09:00:00Z',
  correlationId: 'corr-1',
};

describe('ClaimDetail', () => {
  beforeEach(() => {
    vi.mocked(claimsApi.getClaim).mockResolvedValue(baseClaim);
  });

  it('approves a pending claim', async () => {
    vi.mocked(claimsApi.approveClaim).mockResolvedValue({
      ...baseClaim,
      status: 'APPROVED',
    });
    renderWithQueryClient(<ClaimDetail />);

    const approveButton = await screen.findByRole('button', {
      name: /^approve$/i,
    });
    await userEvent.click(approveButton);

    await waitFor(() => {
      expect(claimsApi.approveClaim).toHaveBeenCalledWith('claim-1', 'adj-1');
    });
  });

  it('rejects a pending claim with the entered reason', async () => {
    vi.mocked(claimsApi.rejectClaim).mockResolvedValue({
      ...baseClaim,
      status: 'REJECTED',
    });
    renderWithQueryClient(<ClaimDetail />);

    await userEvent.click(
      await screen.findByRole('button', { name: /^reject$/i }),
    );
    await userEvent.type(
      screen.getByPlaceholderText(/reason for rejection/i),
      'Duplicate submission',
    );
    await userEvent.click(
      screen.getByRole('button', { name: /confirm reject/i }),
    );

    await waitFor(() => {
      expect(claimsApi.rejectClaim).toHaveBeenCalledWith(
        'claim-1',
        'adj-1',
        'Duplicate submission',
      );
    });
  });

  it('shows an error message when the approve action fails', async () => {
    vi.mocked(claimsApi.approveClaim).mockRejectedValue(
      new Error('downstream failure'),
    );
    renderWithQueryClient(<ClaimDetail />);

    await userEvent.click(
      await screen.findByRole('button', { name: /^approve$/i }),
    );

    expect(
      await screen.findByText('Action failed. Please try again.'),
    ).toBeInTheDocument();
  });
});
