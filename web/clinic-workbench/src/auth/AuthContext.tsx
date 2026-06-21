import { createContext, useState, useCallback, type ReactNode } from 'react'
import { DEMO_USERS, type DemoUser } from './users'

interface AuthContextValue {
  user: DemoUser | null
  login: (email: string, password: string) => boolean
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

const STORAGE_KEY = 'cip_user'

function loadUser(): DemoUser | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as DemoUser) : null
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<DemoUser | null>(loadUser)

  const login = useCallback((email: string, password: string): boolean => {
    const match = DEMO_USERS.find(u => u.email === email && u.password === password)
    if (!match) return false
    setUser(match)
    localStorage.setItem(STORAGE_KEY, JSON.stringify(match))
    return true
  }, [])

  const logout = useCallback(() => {
    setUser(null)
    localStorage.removeItem(STORAGE_KEY)
  }, [])

  return <AuthContext.Provider value={{ user, login, logout }}>{children}</AuthContext.Provider>
}

export { AuthContext }
export type { AuthContextValue }
