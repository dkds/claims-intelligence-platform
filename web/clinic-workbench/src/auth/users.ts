export type Role = 'clinic_manager' | 'adjuster'

export interface DemoUser {
  id: string
  email: string
  password: string
  role: Role
  name: string
  clinicId: string
}

export const DEMO_USERS: DemoUser[] = [
  {
    id: '00000000-0000-0000-0000-000000000001',
    email: 'manager@clinic.com',
    password: 'demo',
    role: 'clinic_manager',
    name: 'Alex Manager',
    clinicId: '00000000-0000-0000-0000-0000000000c1',
  },
  {
    id: '00000000-0000-0000-0000-000000000002',
    email: 'adjuster@clinic.com',
    password: 'demo',
    role: 'adjuster',
    name: 'Sam Adjuster',
    clinicId: '00000000-0000-0000-0000-0000000000c1',
  },
]
