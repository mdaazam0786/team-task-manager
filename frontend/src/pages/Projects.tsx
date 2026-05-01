import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, type ProjectResponse } from '../api'
import { useAuth } from '../auth-context'
import { Card, ErrorBanner } from '../ui'

export function ProjectsPage() {
  const { me } = useAuth()
  const [projects, setProjects] = useState<ProjectResponse[]>([])
  const [error, setError] = useState<string | null>(null)

  const sorted = useMemo(() => [...projects].sort((a, b) => (a.createdAt < b.createdAt ? 1 : -1)), [projects])

  async function load() {
    setError(null)
    try {
      setProjects(await api.listProjects())
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load projects')
    }
  }

  useEffect(() => {
    let mounted = true
    Promise.resolve().then(() => mounted && setError(null))
    api
      .listProjects()
      .then((p) => mounted && setProjects(p))
      .catch((e) => mounted && setError(e instanceof Error ? e.message : 'Failed to load projects'))
    const onRefresh = () => {
      void load()
    }
    window.addEventListener('projects:refresh', onRefresh)
    return () => {
      mounted = false
      window.removeEventListener('projects:refresh', onRefresh)
    }
  }, [])

  return (
    <div className="container page">
      <div className="row">
        <h1>Projects</h1>
        <button className="btn subtle" onClick={load}>
          Refresh
        </button>
      </div>
      <ErrorBanner message={error} />

      <Card>
        <h2>My projects</h2>
        <div className="list">
          {sorted.length === 0 ? (
            <div className="muted">No projects yet. Click the + button to create one.</div>
          ) : (
            sorted.map((p) => {
              const myRole = p.members.find((m) => m.userId === me?.userId)?.role ?? 'MEMBER'
              return (
                <Link key={p.id} to={`/projects/${p.id}`} className="listItem listItemLink">
                  <div>
                    <div className="listTitle">{p.name}</div>
                    <div className="muted small">
                      {myRole} • {p.members.length} member{p.members.length === 1 ? '' : 's'}
                    </div>
                  </div>
                </Link>
              )
            })
          )}
        </div>
      </Card>
    </div>
  )
}

