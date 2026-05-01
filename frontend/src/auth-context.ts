import { createContext, useContext } from 'react'
import type { AuthResponse } from './api'

export type AuthCtx = {
  me: AuthResponse | null
  setSession: (auth: AuthResponse) => void
  logout: () => void
}

export const AuthContext = createContext<AuthCtx | null>(null)

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('AuthProvider missing')
  return ctx
}

