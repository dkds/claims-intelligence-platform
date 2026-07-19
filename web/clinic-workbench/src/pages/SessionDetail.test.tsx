import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { renderWithQueryClient } from '../test/render-utils';
import { SessionDetail } from './SessionDetail';
import * as sessionsApi from '../api/sessions';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', () => ({
  useParams: () => ({ id: 'session-1' }),
  useNavigate: () => mockNavigate,
}));

vi.mock('../auth/useAuth', () => ({
  useAuth: () => ({
    user: {
      id: 'mgr-1',
      email: 'manager@clinic.com',
      role: 'clinic_manager',
      name: 'Alex Manager',
      clinicId: 'clinic-1',
    },
  }),
}));

vi.mock('../api/sessions');

const baseSession = {
  _id: 'session-1',
  petId: 'pet-1',
  vetId: 'vet-1',
  clinicId: 'clinic-1',
  status: 'LOGGED',
  lines: [],
  loggedAt: '2026-06-01T09:00:00Z',
};

describe('SessionDetail', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
    vi.mocked(sessionsApi.getSession).mockResolvedValue(baseSession);
  });

  it('verifies a logged session and navigates back to the sessions list', async () => {
    vi.mocked(sessionsApi.verifySession).mockResolvedValue({
      ...baseSession,
      status: 'VERIFIED',
    });
    renderWithQueryClient(<SessionDetail />);

    const verifyButton = await screen.findByRole('button', {
      name: /verify session/i,
    });
    await userEvent.click(verifyButton);

    await waitFor(() => {
      expect(sessionsApi.verifySession).toHaveBeenCalledWith(
        'session-1',
        'mgr-1',
      );
      expect(mockNavigate).toHaveBeenCalledWith('/sessions');
    });
  });

  it('shows an error message when verification fails', async () => {
    vi.mocked(sessionsApi.verifySession).mockRejectedValue(
      new Error('downstream failure'),
    );
    renderWithQueryClient(<SessionDetail />);

    const verifyButton = await screen.findByRole('button', {
      name: /verify session/i,
    });
    await userEvent.click(verifyButton);

    expect(
      await screen.findByText('Verification failed. Please try again.'),
    ).toBeInTheDocument();
    expect(mockNavigate).not.toHaveBeenCalled();
  });
});
