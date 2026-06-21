export type Role = 'clinic_manager' | 'adjuster'

export interface DemoUser {
  email: string
  password: string
  role: Role
  name: string
  clinicId: string
}

export const DEMO_USERS: DemoUser[] = [
  {
    email: 'manager@clinic.com',
    password: 'demo',
    role: 'clinic_manager',
    name: 'Alex Manager',
    clinicId: 'clinic-001',
  },
  {
    email: 'adjuster@clinic.com',
    password: 'demo',
    role: 'adjuster',
    name: 'Sam Adjuster',
    clinicId: 'clinic-001',
  },
]
