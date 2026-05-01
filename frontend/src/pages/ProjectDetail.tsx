import { useCallback, useEffect, useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import { api, type ProjectDetailResponse, type ProjectResponse, type TaskResponse, type TaskStatus } from '../api'
import { useAuth } from '../auth-context'
import { Card, ErrorBanner, Field } from '../ui'

export function ProjectDetailPage() {
  const { projectId } = useParams()
  const { me } = useAuth()
  const [project, setProject] = useState<ProjectDetailResponse | null>(null)
  const [tasks, setTasks] = useState<TaskResponse[]>([])
  const [error, setError] = useState<string | null>(null)
  const [memberEmail, setMemberEmail] = useState('')
  const [memberRole, setMemberRole] = useState<'ADMIN' | 'MEMBER'>('MEMBER')
  const [busy, setBusy] = useState(false)
  const [taskPage, setTaskPage] = useState(0)
  const taskSize = 20

  // Edit task state
  const [editingTask, setEditingTask] = useState<TaskResponse | null>(null)
  const [editTitle, setEditTitle] = useState('')
  const [editDescription, setEditDescription] = useState('')
  const [editAssigneeEmail, setEditAssigneeEmail] = useState('')
  const [editDueDate, setEditDueDate] = useState('')
  const [editBusy, setEditBusy] = useState(false)
  const [editErr, setEditErr] = useState<string | null>(null)
  function mergeProject(base: ProjectResponse, prev: ProjectDetailResponse | null): ProjectDetailResponse {
    return {
      ...base,
      tasks: prev?.tasks ?? [],
      taskTotalCount: prev?.taskTotalCount ?? 0,
      taskTotalPages: prev?.taskTotalPages ?? 0,
      taskCurrentPage: prev?.taskCurrentPage ?? 0,
      taskPageSize: prev?.taskPageSize ?? taskSize,
    }
  }

  const myRole = useMemo(() => project?.members.find((m) => m.userId === me?.userId)?.role, [project, me?.userId])
  const isAdmin = myRole === 'ADMIN'

  // memberMap no longer needed — backend now returns assigneeName directly on each task

  const load = useCallback(async () => {
    if (!projectId) return
    setError(null)
    try {
      const p = await api.getProject(projectId, { taskPage, taskSize })
      setProject(p)
      setTasks(p.tasks ?? [])
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load project')
    }
  }, [projectId, taskPage, taskSize])

  useEffect(() => {
    if (!projectId) return
    let mounted = true
    api
      .getProject(projectId, { taskPage, taskSize })
      .then((p) => {
        if (!mounted) return
        setProject(p)
        setTasks(p.tasks ?? [])
      })
      .catch((e) => mounted && setError(e instanceof Error ? e.message : 'Failed to load project'))
    const onRefresh = (evt: Event) => {
      const e = evt as CustomEvent<{ projectId?: string }>
      if (!e.detail?.projectId || e.detail.projectId !== projectId) return
      void load()
    }
    window.addEventListener('project:refresh', onRefresh as EventListener)
    return () => {
      mounted = false
      window.removeEventListener('project:refresh', onRefresh as EventListener)
    }
  }, [projectId, taskPage, taskSize, load])

  async function setStatus(taskId: string, status: TaskStatus) {
    if (!projectId) return
    setError(null)
    try {
      const updated = await api.updateTaskStatus(projectId, taskId, status)
      setTasks((prev) => prev.map((t) => (t.id === taskId ? updated : t)))
      window.dispatchEvent(new CustomEvent('dashboard:refresh'))
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to update status')
    }
  }

  function openEdit(task: TaskResponse) {
    setEditingTask(task)
    setEditTitle(task.title)
    setEditDescription(task.description ?? '')
    // Pre-fill with the assignee's email (looked up from members), not the raw userId
    const assigneeMember = project?.members.find((m) => m.userId === task.assigneeId)
    setEditAssigneeEmail(assigneeMember?.email ?? '')
    setEditDueDate(task.dueDate ?? '')
    setEditErr(null)
  }

  function closeEdit() {
    setEditingTask(null)
    setEditErr(null)
  }

  async function saveEdit(e: React.FormEvent) {
    e.preventDefault()
    if (!projectId || !editingTask) return
    if (!editTitle.trim()) { setEditErr('Title is required.'); return }
    setEditBusy(true)
    setEditErr(null)
    try {
      const updated = await api.updateTask(projectId, editingTask.id, {
        title: editTitle.trim(),
        description: editDescription.trim() || undefined,
        assigneeEmail: editAssigneeEmail.trim() || undefined,
        dueDate: editDueDate || undefined,
      })
      setTasks((prev) => prev.map((t) => (t.id === updated.id ? updated : t)))
      closeEdit()
      window.dispatchEvent(new CustomEvent('dashboard:refresh'))
    } catch (e) {
      setEditErr(e instanceof Error ? e.message : 'Failed to update task')
    } finally {
      setEditBusy(false)
    }
  }

  async function deleteTask(taskId: string) {
    if (!projectId) return
    if (!window.confirm('Delete this task? This cannot be undone.')) return
    setError(null)
    try {
      await api.deleteTask(projectId, taskId)
      setTasks((prev) => prev.filter((t) => t.id !== taskId))
      window.dispatchEvent(new CustomEvent('dashboard:refresh'))
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to delete task')
    }
  }

  async function addMember(e: React.FormEvent) {
    e.preventDefault()
    if (!projectId) return
    setBusy(true)
    setError(null)
    try {
      const p = await api.addMember(projectId, memberEmail, memberRole)
      setProject((prev) => mergeProject(p, prev))
      setMemberEmail('')
      setMemberRole('MEMBER')
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to add member')
    } finally {
      setBusy(false)
    }
  }

  async function removeMember(userId: string) {
    if (!projectId) return
    setBusy(true)
    setError(null)
    try {
      const p = await api.removeMember(projectId, userId)
      setProject((prev) => mergeProject(p, prev))
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to remove member')
    } finally {
      setBusy(false)
    }
  }

  const sortedTasks = useMemo(() => {
    const copy = [...tasks]
    copy.sort((a, b) => (a.createdAt < b.createdAt ? 1 : -1))
    return copy
  }, [tasks])

  const grouped = useMemo(() => {
    const todo = sortedTasks.filter((t) => t.status === 'TODO')
    const inProgress = sortedTasks.filter((t) => t.status === 'IN_PROGRESS')
    const done = sortedTasks.filter((t) => t.status === 'DONE')
    return { todo, inProgress, done }
  }, [sortedTasks])

  const totalPages = project?.taskTotalPages ?? 1

  // Compute overdue: tasks not DONE with a dueDate in the past
  const overdueCount = useMemo(() => {
    const now = new Date().toISOString().slice(0, 10)
    return sortedTasks.filter((t) => t.status !== 'DONE' && t.dueDate && t.dueDate < now).length
  }, [sortedTasks])

  return (
    <div className="container page">
      <div className="row">
        <h1>{project?.name ?? 'Project'}</h1>
        <button className="btn subtle" onClick={load}>
          Refresh
        </button>
      </div>
      {project?.description ? <p className="muted">{project.description}</p> : null}
      <ErrorBanner message={error} />

      <div className="stack">
        <Card>
          <div className="row">
            <h2>Tasks</h2>
            <div className="muted small">
              TODO {grouped.todo.length} · In progress {grouped.inProgress.length} · Done {grouped.done.length}
              {overdueCount > 0 ? <span className="overdueChip"> · ⚠ {overdueCount} overdue</span> : null}
            </div>
          </div>

          {sortedTasks.length === 0 ? (
            <div className="muted">No tasks yet. Click the + button to create one.</div>
          ) : (
            <div className="list">
              {sortedTasks.map((t) => {
                const isOverdue =
                  t.status !== 'DONE' && !!t.dueDate && t.dueDate < new Date().toISOString().slice(0, 10)
                return (
                  <div key={t.id} className={`task ${isOverdue ? 'taskOverdue' : ''}`}>
                    <div className="taskHead">
                      <div className="listTitle">{t.title}</div>
                      <div className="taskHeadRight">
                        {isOverdue ? <span className="pill pillDanger">Overdue</span> : null}
                        <span className={`pill ${t.status}`}>{t.status.replace('_', ' ')}</span>
                      </div>
                    </div>
                    {t.description ? <div className="muted small">{t.description}</div> : null}
                    <div className="muted small taskMeta">
                      <span>Assignee: {t.assigneeName ?? '—'}</span>
                      <span>Due: {t.dueDate ?? '—'}</span>
                    </div>
                    <div className="actions">
                      <button
                        className={`btn small subtle ${t.status === 'TODO' ? 'statusActive' : ''}`}
                        onClick={() => setStatus(t.id, 'TODO')}
                      >
                        TODO
                      </button>
                      <button
                        className={`btn small subtle ${t.status === 'IN_PROGRESS' ? 'statusActive' : ''}`}
                        onClick={() => setStatus(t.id, 'IN_PROGRESS')}
                      >
                        In progress
                      </button>
                      <button
                        className={`btn small subtle ${t.status === 'DONE' ? 'statusActive' : ''}`}
                        onClick={() => setStatus(t.id, 'DONE')}
                      >
                        Done
                      </button>
                      <button className="btn small subtle" onClick={() => openEdit(t)}>
                        Edit
                      </button>
                      <button className="btn small danger" onClick={() => deleteTask(t.id)}>
                        Delete
                      </button>
                    </div>
                  </div>
                )
              })}
            </div>
          )}

          {/* Pagination */}
          {totalPages > 1 ? (
            <div className="pagination">
              <button
                className="btn small subtle"
                disabled={taskPage === 0}
                onClick={() => setTaskPage((p) => Math.max(0, p - 1))}
              >
                ← Prev
              </button>
              <span className="muted small">
                Page {taskPage + 1} of {totalPages}
              </span>
              <button
                className="btn small subtle"
                disabled={taskPage >= totalPages - 1}
                onClick={() => setTaskPage((p) => p + 1)}
              >
                Next →
              </button>
            </div>
          ) : null}
        </Card>

        <Card>
          <h2>Team</h2>
          <div className="list">
            {project?.members.map((m) => (
              <div key={m.userId} className="listItem">
                <div>
                  <div className="listTitle">{m.name ?? m.email ?? m.userId}</div>
                  <div className="muted small">{m.role}{m.email ? ` · ${m.email}` : ''}</div>
                </div>
                {isAdmin && m.userId !== me?.userId ? (
                  <button className="btn danger small" disabled={busy} onClick={() => removeMember(m.userId)}>
                    Remove
                  </button>
                ) : null}
              </div>
            ))}
          </div>

          {isAdmin ? (
            <>
              <div className="divider" />
              <h3>Add member</h3>
              <form className="form" onSubmit={addMember}>
                <Field label="User email">
                  <input
                    value={memberEmail}
                    onChange={(e) => setMemberEmail(e.target.value)}
                    placeholder="member@example.com"
                  />
                </Field>
                <Field label="Role">
                  <select value={memberRole} onChange={(e) => setMemberRole(e.target.value as 'ADMIN' | 'MEMBER')}>
                    <option value="MEMBER">MEMBER</option>
                    <option value="ADMIN">ADMIN</option>
                  </select>
                </Field>
                <button className="btn" disabled={busy || !memberEmail.trim()}>
                  Add member
                </button>
              </form>
            </>
          ) : (
            <p className="muted small">Only Admins can add or remove members.</p>
          )}
        </Card>
      </div>

      {/* Edit task modal */}
      {editingTask ? (
        <div className="modalOverlay" role="dialog" aria-modal="true" aria-label="Edit task" onClick={closeEdit}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modalHead">
              <div className="modalTitle">Edit task</div>
              <button className="iconBtn" onClick={closeEdit} aria-label="Close">✕</button>
            </div>
            {editErr ? <div className="error">{editErr}</div> : null}
            <form className="form" onSubmit={saveEdit}>
              <Field label="Title">
                <input value={editTitle} onChange={(e) => setEditTitle(e.target.value)} placeholder="Task title" />
              </Field>
              <Field label="Description (optional)">
                <textarea value={editDescription} onChange={(e) => setEditDescription(e.target.value)} rows={3} />
              </Field>
              <Field label="Assignee (optional)">
                <select value={editAssigneeEmail} onChange={(e) => setEditAssigneeEmail(e.target.value)}>
                  <option value="">— Unassigned —</option>
                  {project?.members.map((m) => (
                    <option key={m.userId} value={m.email ?? ''}>
                      {m.name ?? m.email ?? m.userId}
                    </option>
                  ))}
                </select>
              </Field>
              <Field label="Due date (optional)">
                <input type="date" value={editDueDate} onChange={(e) => setEditDueDate(e.target.value)} />
              </Field>
              <button className="btn" type="submit" disabled={editBusy || !editTitle.trim()}>
                {editBusy ? 'Saving…' : 'Save changes'}
              </button>
            </form>
          </div>
        </div>
      ) : null}
    </div>
  )
}
