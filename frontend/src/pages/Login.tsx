import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api'
import { useAuth } from '../auth-context'
import { Card, ErrorBanner, Field } from '../ui'

function validateEmail(v: string) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v.trim())
}

export function LoginPage() {
  const nav = useNavigate()
  const { setSession } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [touched, setTouched] = useState({ email: false, password: false })
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const emailErr = touched.email && !validateEmail(email) ? 'Enter a valid email address.' : null
  const passwordErr = touched.password && password.length < 6 ? 'Password must be at least 6 characters.' : null

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    setTouched({ email: true, password: true })
    if (!validateEmail(email) || password.length < 6) return
    setError(null)
    setLoading(true)
    try {
      const auth = await api.login(email, password)
      setSession(auth)
      nav('/dashboard')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="container page">
      <Card>
        <h1>Login</h1>
        <p className="muted">
          New here? <Link to="/signup">Create an account</Link>.
        </p>
        <ErrorBanner message={error} />
        <form onSubmit={onSubmit} className="form" noValidate>
          <Field label="Email">
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              onBlur={() => setTouched((t) => ({ ...t, email: true }))}
              placeholder="you@example.com"
              aria-invalid={!!emailErr}
            />
            {emailErr ? <div className="fieldErr">{emailErr}</div> : null}
          </Field>
          <Field label="Password">
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              onBlur={() => setTouched((t) => ({ ...t, password: true }))}
              aria-invalid={!!passwordErr}
            />
            {passwordErr ? <div className="fieldErr">{passwordErr}</div> : null}
          </Field>
          <button disabled={loading} className="btn" type="submit">
            {loading ? 'Signing in…' : 'Login'}
          </button>
        </form>
      </Card>
    </div>
  )
}
