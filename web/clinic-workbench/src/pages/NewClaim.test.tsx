import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { vi, describe, it, expect, beforeEach } from 'vitest'
import { renderWithQueryClient } from '../test/render-utils'
import { NewClaim } from './NewClaim'
import * as claimsApi from '../api/claims'

const mockNavigate = vi.fn()

vi.mock('react-router-dom', () => ({
  useNavigate: () => mockNavigate,
}))

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
}))

vi.mock('../api/claims')

async function fillRequiredFields() {
  const [petIdInput, policyIdInput] = screen.getAllByPlaceholderText('UUID')
  await userEvent.type(petIdInput, ' pet-1 ')
  await userEvent.type(policyIdInput, ' policy-1 ')
  await userEvent.type(screen.getByPlaceholderText('e.g. DENT-CLEAN'), 'DENT-CLEAN')
  await userEvent.type(screen.getByPlaceholderText('0.00'), '49.99')
}

describe('NewClaim', () => {
  beforeEach(() => {
    mockNavigate.mockClear()
  })

  it('submits a manual claim with trimmed and parsed line values, then navigates to the claims list', async () => {
    vi.mocked(claimsApi.submitClaim).mockResolvedValue({ _id: 'claim-1' })
    renderWithQueryClient(<NewClaim />)

    await fillRequiredFields()
    await userEvent.click(screen.getByRole('button', { name: /submit claim/i }))

    await waitFor(() => {
      expect(claimsApi.submitClaim).toHaveBeenCalledWith('clinic-1', {
        petId: 'pet-1',
        policyId: 'policy-1',
        submittedBy: 'mgr-1',
        lines: [{ procedureCode: 'DENT-CLEAN', quantity: 1, requestedAmount: 49.99 }],
      })
      expect(mockNavigate).toHaveBeenCalledWith('/claims')
    })
  })

  it('shows an error message when submission fails', async () => {
    vi.mocked(claimsApi.submitClaim).mockRejectedValue(new Error('downstream failure'))
    renderWithQueryClient(<NewClaim />)

    await fillRequiredFields()
    await userEvent.click(screen.getByRole('button', { name: /submit claim/i }))

    expect(await screen.findByText('Submission failed. Check IDs and try again.')).toBeInTheDocument()
    expect(mockNavigate).not.toHaveBeenCalled()
  })

  it('adds an additional claim line', async () => {
    renderWithQueryClient(<NewClaim />)

    expect(screen.getAllByPlaceholderText('e.g. DENT-CLEAN')).toHaveLength(1)
    await userEvent.click(screen.getByRole('button', { name: /\+ add line/i }))

    expect(screen.getAllByPlaceholderText('e.g. DENT-CLEAN')).toHaveLength(2)
  })
})
