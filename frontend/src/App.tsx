import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from './auth'
import { useAuth } from './auth-context'
import { getToken } from './api'
import { AppShell } from './ui'
import { DashboardPage } from './pages/Dashboard'
import { LoginPage } from './pages/Login'
import { SignupPage } from './pages/Signup'
import { ProjectsPage } from './pages/Projects'
import { ProjectDetailPage } from './pages/ProjectDetail'
import { ProfilePage } from './pages/Profile'

function RequireAuth({ children }: { children: React.ReactNode }) {
  const { me } = useAuth()
  const loc = useLocation()
  // Avoid a one-render race after login/signup where navigation happens
  // before the auth context state has updated, but the token is already stored.
  if (!me && !getToken()) return <Navigate to="/login" state={{ from: loc.pathname }} replace />
  return <>{children}</>
}

function Home() {
  const { me } = useAuth()
  return <Navigate to={me ? '/dashboard' : '/login'} replace />
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />

          <Route
            element={
              <RequireAuth>
                <AppShell />
              </RequireAuth>
            }
          >
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/projects" element={<ProjectsPage />} />
            <Route path="/projects/:projectId" element={<ProjectDetailPage />} />
            <Route path="/profile" element={<ProfilePage />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
