import { useMemo, useState } from 'react'
import type { AuthResponse } from './api'
import { clearAuth, getMe, setAuth } from './api'
import { AuthContext, type AuthCtx } from './auth-context'

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [me, setMe] = useState<AuthResponse | null>(() => getMe())

  const value = useMemo<AuthCtx>(
    () => ({
      me,
      setSession: (auth) => {
        setAuth(auth)
        setMe(auth)
      },
      logout: () => {
        clearAuth()
        setMe(null)
      },
    }),
    [me],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

