import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  listClinics,
  createClinic,
  listVets,
  createVet,
  approveVet,
  rejectVet,
  type CreateClinicPayload,
  type CreateVetPayload,
} from '../api/enrollment'

export const clinicKeys = {
  list: ['clinics'] as const,
}

export const vetKeys = {
  list: (clinicId: string) => ['vets', clinicId] as const,
}

export function useClinics() {
  return useQuery({
    queryKey: clinicKeys.list,
    queryFn: listClinics,
  })
}

export function useCreateClinic() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: CreateClinicPayload) => createClinic(body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: clinicKeys.list })
    },
  })
}

export function useVets(clinicId: string) {
  return useQuery({
    queryKey: vetKeys.list(clinicId),
    queryFn: () => listVets(clinicId),
  })
}

export function useCreateVet(clinicId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: CreateVetPayload) => createVet(clinicId, body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: vetKeys.list(clinicId) })
    },
  })
}

export function useApproveVet(id: string, clinicId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => approveVet(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: vetKeys.list(clinicId) })
    },
  })
}

export function useRejectVet(id: string, clinicId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (reason: string) => rejectVet(id, reason),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: vetKeys.list(clinicId) })
    },
  })
}
