import { Link, NavLink, Outlet, useLocation } from 'react-router-dom'
import { useMemo, useState } from 'react'
import { useAuth } from './auth-context'
import { api } from './api'

function Icon({
  name,
}: {
  name:
    | 'grid'
    | 'folder'
    | 'calendar'
    | 'chat'
    | 'settings'
    | 'search'
    | 'bell'
    | 'plus'
    | 'logout'
    | 'user'
}) {
  // Simple inline icons to avoid extra deps
  const common = { width: 18, height: 18, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 2 }
  switch (name) {
    case 'grid':
      return (
        <svg {...common} aria-hidden="true">
          <path d="M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z" />
        </svg>
      )
    case 'folder':
      return (
        <svg {...common} aria-hidden="true">
          <path d="M3 7a2 2 0 0 1 2-2h5l2 2h9a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
        </svg>
      )
    case 'calendar':
      return (
        <svg {...common} aria-hidden="true">
          <path d="M8 3v3M16 3v3" />
          <path d="M4 7h16" />
          <path d="M6 11h4M6 15h4M14 11h4M14 15h4" />
          <path d="M5 5h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z" />
        </svg>
      )
    case 'chat':
      return (
        <svg {...common} aria-hidden="true">
          <path d="M21 15a4 4 0 0 1-4 4H8l-5 3V7a4 4 0 0 1 4-4h10a4 4 0 0 1 4 4z" />
        </svg>
      )
    case 'settings':
      return (
        <svg {...common} aria-hidden="true">
          <path d="M12 15.5a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7z" />
          <path d="M19.4 15a7.7 7.7 0 0 0 .1-2l2-1.5-2-3.5-2.4.8a7.6 7.6 0 0 0-1.7-1l-.4-2.5h-4l-.4 2.5c-.6.2-1.2.6-1.7 1L4.5 8l-2 3.5L4.5 13c0 .7 0 1.4.1 2l-2 1.5 2 3.5 2.4-.8c.5.4 1.1.8 1.7 1l.4 2.6h4l.4-2.6c.6-.2 1.2-.6 1.7-1l2.4.8 2-3.5z" />
        </svg>
      )
    case 'search':
      return (
        <svg {...common} aria-hidden="true">
          <path d="M10 18a8 8 0 1 1 0-16 8 8 0 0 1 0 16z" />
          <path d="M21 21l-4.3-4.3" />
        </svg>
      )
    case 'bell':
      return (
        <svg {...common} aria-hidden="true">
          <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 7h18s-3 0-3-7" />
          <path d="M13.7 21a2 2 0 0 1-3.4 0" />
        </svg>
      )
    case 'plus':
      return (
        <svg {...common} aria-hidden="true">
          <path d="M12 5v14M5 12h14" />
        </svg>
      )
    case 'logout':
      return (
        <svg {...common} aria-hidden="true">
          <path d="M10 17l5-5-5-5" />
          <path d="M15 12H3" />
          <path d="M21 3v18" />
        </svg>
      )
    case 'user':
      return (
        <svg {...common} aria-hidden="true">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
          <circle cx="12" cy="7" r="4" />
        </svg>
      )
  }
}

function SidebarLink({
  to,
  icon,
  label,
}: {
  to: string
  icon: Parameters<typeof Icon>[0]['name']
  label: string
}) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) => `sidebarLink ${isActive ? 'active' : ''}`}
      end
      aria-label={label}
      title={label}
    >
      <span className="sidebarIcon" aria-hidden="true">
        <Icon name={icon} />
      </span>
      <span className="sidebarLabel">{label}</span>
    </NavLink>
  )
}

export function AppShell() {
  const { me, logout } = useAuth()
  const loc = useLocation()
  const [showModal, setShowModal] = useState(false)
  const [createName, setCreateName] = useState('')
  const [createDescription, setCreateDescription] = useState('')
  const [taskTitle, setTaskTitle] = useState('')
  const [taskDescription, setTaskDescription] = useState('')
  const [taskAssigneeEmail, setTaskAssigneeEmail] = useState('')
  const [taskDueDate, setTaskDueDate] = useState('')
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState<string | null>(null)
  const [projectMembers, setProjectMembers] = useState<Array<{ userId: string; name: string | null; email: string | null }>>([])

  const projectDetailMatch = useMemo(() => loc.pathname.match(/^\/projects\/([^/]+)$/), [loc.pathname])
  const projectIdForTask = projectDetailMatch?.[1] ?? null

  const action = useMemo(() => {
    if (loc.pathname === '/projects') return 'createProject' as const
    if (projectIdForTask) return 'createTask' as const
    return null
  }, [loc.pathname, projectIdForTask])

  const modalTitle = useMemo(() => {
    if (action === 'createProject') return 'Create project'
    if (action === 'createTask') return 'Create task'
    return 'Dialog'
  }, [action])

  function openModal() {
    setErr(null)
    setShowModal(true)
    // Fetch project members so the assignee dropdown is populated
    if (projectIdForTask) {
      api.getProject(projectIdForTask).then((p) => setProjectMembers(p.members)).catch(() => {})
    }
  }

  function closeModal() {
    setShowModal(false)
    setBusy(false)
    setErr(null)
    setProjectMembers([])
  }

  async function onPrimaryAction() {
    setBusy(true)
    setErr(null)
    try {
      if (action === 'createProject') {
        await api.createProject(createName.trim(), createDescription.trim())
        setCreateName('')
        setCreateDescription('')
        closeModal()
        window.dispatchEvent(new CustomEvent('projects:refresh'))
        return
      }

      if (action === 'createTask' && projectIdForTask) {
        await api.createTask(projectIdForTask, {
          title: taskTitle.trim(),
          description: taskDescription.trim() || undefined,
          assigneeEmail: taskAssigneeEmail.trim() || undefined,
          dueDate: taskDueDate || undefined,
        })
        setTaskTitle('')
        setTaskDescription('')
        setTaskAssigneeEmail('')
        setTaskDueDate('')
        closeModal()
        window.dispatchEvent(new CustomEvent('project:refresh', { detail: { projectId: projectIdForTask } }))
        window.dispatchEvent(new CustomEvent('dashboard:refresh'))
        return
      }
    } catch (e) {
      setErr(e instanceof Error ? e.message : 'Request failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="appShell">
      <aside className="sidebar" aria-label="Primary">
        <div className="sidebarTop">
          <Link to="/dashboard" className="sidebarBrand" aria-label="Home">
            <img src="/favicon.svg" alt="Logo" className="sidebarLogoImg" aria-hidden="true" />
          </Link>
        </div>

        <nav className="sidebarNav">
          <SidebarLink to="/dashboard" icon="grid" label="Dashboard" />
          <SidebarLink to="/projects" icon="folder" label="Projects" />
          <SidebarLink to="/profile" icon="user" label="Profile" />
          <a className="sidebarLink disabled" aria-disabled="true" title="Coming soon">
            <span className="sidebarIcon" aria-hidden="true">
              <Icon name="calendar" />
            </span>
            <span className="sidebarLabel">Schedule</span>
          </a>
          <a className="sidebarLink disabled" aria-disabled="true" title="Coming soon">
            <span className="sidebarIcon" aria-hidden="true">
              <Icon name="chat" />
            </span>
            <span className="sidebarLabel">Messages</span>
          </a>
          <a className="sidebarLink disabled" aria-disabled="true" title="Coming soon">
            <span className="sidebarIcon" aria-hidden="true">
              <Icon name="settings" />
            </span>
            <span className="sidebarLabel">Settings</span>
          </a>
        </nav>

        <div className="sidebarBottom">
          <div className="sidebarMe">
            <div className="avatar" aria-hidden="true">
              {(me?.name ?? me?.email ?? '?').slice(0, 1).toUpperCase()}
            </div>
            <div className="sidebarMeText">
              <div className="sidebarMeName">{me?.name ?? 'Account'}</div>
              <div className="sidebarMeEmail">{me?.email ?? ''}</div>
            </div>
          </div>
          <button className="iconBtn" onClick={logout} title="Logout" aria-label="Logout">
            <Icon name="logout" />
          </button>
          <div className="sidebarHint muted small">Signed in • {loc.pathname}</div>
        </div>
      </aside>

      <main className="shellMain">
        <div className="shellActions" aria-label="Top actions">
          <button className="iconBtn" title="Notifications" aria-label="Notifications">
            <Icon name="bell" />
          </button>
          <button
            className="primaryIconBtn"
            title={action === 'createProject' ? 'Create project' : action === 'createTask' ? 'Create task' : 'New'}
            aria-label={action === 'createProject' ? 'Create project' : action === 'createTask' ? 'Create task' : 'New'}
            onClick={() => {
              openModal()
            }}
          >
            <Icon name="plus" />
          </button>
        </div>

        <div className="shellContent">
          <Outlet />
        </div>
      </main>

      {showModal ? (
        <div className="modalOverlay" role="dialog" aria-modal="true" aria-label={modalTitle} onClick={closeModal}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            {action === 'createProject' ? (
              <>
                <div className="modalHead">
                  <div className="modalTitle">Create project</div>
                  <button className="iconBtn" onClick={closeModal} aria-label="Close">
                    ✕
                  </button>
                </div>
                {err ? <div className="error">{err}</div> : null}
                <div className="form">
                  <label className="field">
                    <div className="label">Name</div>
                    <input value={createName} onChange={(e) => setCreateName(e.target.value)} placeholder="Project name" />
                  </label>
                  <label className="field">
                    <div className="label">Description (optional)</div>
                    <textarea value={createDescription} onChange={(e) => setCreateDescription(e.target.value)} rows={3} />
                  </label>
                  <button className="btn" disabled={busy || !createName.trim()} onClick={onPrimaryAction}>
                    {busy ? 'Creating…' : 'Create'}
                  </button>
                </div>
              </>
            ) : action === 'createTask' ? (
              <>
                <div className="modalHead">
                  <div className="modalTitle">Create task</div>
                  <button className="iconBtn" onClick={closeModal} aria-label="Close">
                    ✕
                  </button>
                </div>
                {err ? <div className="error">{err}</div> : null}
                <div className="form">
                  <label className="field">
                    <div className="label">Title</div>
                    <input value={taskTitle} onChange={(e) => setTaskTitle(e.target.value)} placeholder="Task title" />
                  </label>
                  <label className="field">
                    <div className="label">Description (optional)</div>
                    <textarea value={taskDescription} onChange={(e) => setTaskDescription(e.target.value)} rows={3} />
                  </label>
                  <label className="field">
                    <div className="label">Assignee (optional)</div>
                    <select value={taskAssigneeEmail} onChange={(e) => setTaskAssigneeEmail(e.target.value)}>
                      <option value="">— Unassigned —</option>
                      {projectMembers.map((m) => (
                        <option key={m.userId} value={m.email ?? ''}>
                          {m.name ?? m.email ?? m.userId}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label className="field">
                    <div className="label">Due date (optional)</div>
                    <input type="date" value={taskDueDate} onChange={(e) => setTaskDueDate(e.target.value)} />
                  </label>
                  <button className="btn" disabled={busy || !taskTitle.trim()} onClick={onPrimaryAction}>
                    {busy ? 'Creating…' : 'Create'}
                  </button>
                </div>
              </>
            ) : (
              <>
                <div className="modalHead">
                  <div className="modalTitle">Not available</div>
                  <button className="iconBtn" onClick={closeModal} aria-label="Close">
                    ✕
                  </button>
                </div>
                <div className="muted">This action is only enabled on the Projects page for now.</div>
              </>
            )}
          </div>
        </div>
      ) : null}
    </div>
  )
}

export function Card({ children }: { children: React.ReactNode }) {
  return <div className="card">{children}</div>
}

export function Field({
  label,
  children,
}: {
  label: string
  children: React.ReactNode
}) {
  return (
    <label className="field">
      <div className="label">{label}</div>
      {children}
    </label>
  )
}

export function ErrorBanner({ message }: { message: string | null }) {
  if (!message) return null
  return <div className="error">{message}</div>
}

