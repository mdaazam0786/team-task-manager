import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api'
import { useAuth } from '../auth-context'
import { Card, ErrorBanner, Field } from '../ui'

function validateEmail(v: string) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v.trim())
}

export function SignupPage() {
  const nav = useNavigate()
  const { setSession } = useAuth()
  const [email, setEmail] = useState('')
  const [name, setName] = useState('')
  const [password, setPassword] = useState('')
  const [touched, setTouched] = useState({ email: false, name: false, password: false })
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const emailErr = touched.email && !validateEmail(email) ? 'Enter a valid email address.' : null
  const nameErr = touched.name && name.trim().length < 2 ? 'Name must be at least 2 characters.' : null
  const passwordErr = touched.password && password.length < 6 ? 'Password must be at least 6 characters.' : null

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    setTouched({ email: true, name: true, password: true })
    if (!validateEmail(email) || name.trim().length < 2 || password.length < 6) return
    setError(null)
    setLoading(true)
    try {
      const auth = await api.signup(email, name.trim(), password)
      setSession(auth)
      nav('/dashboard')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Signup failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="container page">
      <Card>
        <h1>Create account</h1>
        <p className="muted">
          Already have an account? <Link to="/login">Login</Link>.
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
          <Field label="Name">
            <input
              value={name}
              onChange={(e) => setName(e.target.value)}
              onBlur={() => setTouched((t) => ({ ...t, name: true }))}
              placeholder="Your name"
              aria-invalid={!!nameErr}
            />
            {nameErr ? <div className="fieldErr">{nameErr}</div> : null}
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
            {loading ? 'Creating…' : 'Signup'}
          </button>
        </form>
      </Card>
    </div>
  )
}
