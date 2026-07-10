import client from './client'

export interface Clinic {
  _id: string
  name: string
  contactEmail?: string
  status: string
  updatedAt: string
}

export interface CreateClinicPayload {
  name: string
  contactEmail?: string
  addressLine1?: string
  city?: string
  postcode?: string
  countryCode?: string
  contactPhone?: string
}

export interface Vet {
  _id: string
  clinicId: string
  firstName: string
  lastName: string
  status: string
  rejectionReason?: string
}

export interface CreateVetPayload {
  firstName: string
  lastName: string
  licenseNumber: string
  email?: string
}

export const listClinics = () => client.get<Clinic[]>('/clinics').then(r => r.data)

export const createClinic = (body: CreateClinicPayload) =>
  client.post('/clinics', body).then(r => r.data)

export const listVets = (clinicId: string) =>
  client.get<Vet[]>(`/clinics/${clinicId}/vets`).then(r => r.data)

export const createVet = (clinicId: string, body: CreateVetPayload) =>
  client.post(`/clinics/${clinicId}/vets`, body).then(r => r.data)

export const approveVet = (id: string) => client.post(`/vets/${id}/approve`).then(r => r.data)

export const rejectVet = (id: string, reason: string) =>
  client.post(`/vets/${id}/reject`, { reason }).then(r => r.data)
