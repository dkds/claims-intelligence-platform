import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  listClaims,
  getClaim,
  submitClaim,
  approveClaim,
  rejectClaim,
  type SubmitClaimBody,
} from '../api/claims'

export const claimKeys = {
  list: (clinicId: string, status?: string) => ['claims', clinicId, status] as const,
  detail: (id: string) => ['claims', 'detail', id] as const,
}

export function useClaims(clinicId: string, status?: string) {
  return useQuery({
    queryKey: claimKeys.list(clinicId, status),
    queryFn: () => listClaims(clinicId, status),
  })
}

export function useClaim(id: string) {
  return useQuery({
    queryKey: claimKeys.detail(id),
    queryFn: () => getClaim(id),
  })
}

export function useSubmitClaim(clinicId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: SubmitClaimBody) => submitClaim(clinicId, body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['claims', clinicId] })
    },
  })
}

export function useApproveClaim(id: string, clinicId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (approvedBy: string) => approveClaim(id, approvedBy),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: claimKeys.detail(id) })
      qc.invalidateQueries({ queryKey: ['claims', clinicId] })
    },
  })
}

export function useRejectClaim(id: string, clinicId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (vars: { rejectedBy: string; reason?: string }) =>
      rejectClaim(id, vars.rejectedBy, vars.reason),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: claimKeys.detail(id) })
      qc.invalidateQueries({ queryKey: ['claims', clinicId] })
    },
  })
}
