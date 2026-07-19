import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { renderWithQueryClient } from '../test/render-utils';
import { NewClinic } from './NewClinic';
import * as enrollmentApi from '../api/enrollment';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', () => ({
  useNavigate: () => mockNavigate,
}));

vi.mock('../api/enrollment');

describe('NewClinic', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
  });

  it('registers a clinic with trimmed fields and navigates to the clinics list', async () => {
    vi.mocked(enrollmentApi.createClinic).mockResolvedValue({
      _id: 'clinic-1',
    });
    renderWithQueryClient(<NewClinic />);

    const [nameInput] = screen.getAllByRole('textbox');
    await userEvent.type(nameInput, ' Riverside Vets ');
    await userEvent.click(
      screen.getByRole('button', { name: /register clinic/i }),
    );

    await waitFor(() => {
      expect(enrollmentApi.createClinic).toHaveBeenCalledWith({
        name: 'Riverside Vets',
        contactEmail: undefined,
        city: undefined,
        postcode: undefined,
      });
      expect(mockNavigate).toHaveBeenCalledWith('/clinics');
    });
  });

  it('shows an error message when registration fails', async () => {
    vi.mocked(enrollmentApi.createClinic).mockRejectedValue(
      new Error('downstream failure'),
    );
    renderWithQueryClient(<NewClinic />);

    const [nameInput] = screen.getAllByRole('textbox');
    await userEvent.type(nameInput, 'Riverside Vets');
    await userEvent.click(
      screen.getByRole('button', { name: /register clinic/i }),
    );

    expect(
      await screen.findByText('Registration failed. Please try again.'),
    ).toBeInTheDocument();
    expect(mockNavigate).not.toHaveBeenCalled();
  });
});
