import client from './client'

export interface ClaimLine {
  lineId: string
  description?: string
  amount: number
  category: string
}

export interface FraudResult {
  score: number
  riskLevel: string
  flags: string[]
  modelVersion: string
  scoredAt: string
}

export interface Claim {
  _id: string
  clinicId: string
  petId: string
  policyId: string
  origin: string
  sourceSessionId?: string
  status: string
  submittedBy: string
  totalRequested: number
  lines: ClaimLine[]
  fraud?: FraudResult
  decision?: string
  approvedAmount?: number
  reasons?: string[]
  adjudicatedBy?: string
  assembledAt: string
  updatedAt: string
  correlationId: string
}

export interface ManualClaimLine {
  procedureCode: string
  quantity: number
  requestedAmount: number
}

export interface SubmitClaimBody {
  petId: string
  policyId: string
  submittedBy: string
  lines: ManualClaimLine[]
}

export const listClaims = (clinicId: string, status?: string) =>
  client
    .get<Claim[]>(`/clinics/${clinicId}/claims`, { params: status ? { status } : undefined })
    .then(r => r.data)

export const getClaim = (id: string) =>
  client.get<Claim>(`/claims/${id}`).then(r => r.data)

export const submitClaim = (clinicId: string, body: SubmitClaimBody) =>
  client.post(`/clinics/${clinicId}/claims`, body).then(r => r.data)
