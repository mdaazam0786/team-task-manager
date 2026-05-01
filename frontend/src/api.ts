export type AuthResponse = {
  token: string
  userId: string
  email: string
  name: string
}

export type ProjectRole = 'ADMIN' | 'MEMBER'
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE'

export type ProjectMemberResponse = {
  userId: string
  email: string | null
  name: string | null
  role: ProjectRole
}

export type ProjectResponse = {
  id: string
  name: string
  description: string | null
  members: ProjectMemberResponse[]
  createdAt: string
}

export type ProjectDetailResponse = ProjectResponse & {
  tasks: TaskResponse[]
  taskTotalCount: number
  taskTotalPages: number
  taskCurrentPage: number
  taskPageSize: number
}

export type TaskResponse = {
  id: string
  projectId: string
  title: string
  description: string | null
  status: TaskStatus
  assigneeId: string | null
  assigneeName: string | null
  dueDate: string | null
  createdBy: string
  createdAt: string
  updatedAt: string
}

export type DashboardResponse = {
  // Legacy shape (kept for backward compatibility)
  todo: number
  inProgress: number
  done: number
  overdue: number
}

export type DashboardOverviewResponse = {
  tasksDone: number
  tasksInProgress: number
  projectsDone: number
  newTasks: number
  overdue?: number
}

// Dashboard chart range selector
export type DashboardRange = 'daily' | 'weekly' | 'monthly'

export type ProfileResponse = {
  id: string
  name: string
  email: string
  bio: string | null
  createdAt: string
}

const TOKEN_KEY = 'ttm_token'
const USER_KEY = 'ttm_user'

export function getToken() {
  const raw = localStorage.getItem(TOKEN_KEY)
  if (!raw) return null
  // Guard against accidentally storing "undefined"/"null" as strings
  if (raw === 'undefined' || raw === 'null') return null
  return raw
}

export function setAuth(auth: AuthResponse) {
  if (!auth.token || auth.token === 'undefined' || auth.token === 'null') {
    clearAuth()
    return
  }
  localStorage.setItem(TOKEN_KEY, auth.token)
  localStorage.setItem(USER_KEY, JSON.stringify(auth))
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export function getMe(): AuthResponse | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as AuthResponse
  } catch {
    return null
  }
}

// In production (Railway), VITE_API_URL is set to the backend service URL.
// In dev, it's empty so requests go to localhost:8080 via the Vite proxy.
const API_BASE = (import.meta.env.VITE_API_URL as string | undefined)?.replace(/\/$/, '') ?? ''

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers ?? {})
  headers.set('Content-Type', 'application/json')
  const token = getToken()
  if (token) headers.set('Authorization', `Bearer ${token}`)

  const res = await fetch(`${API_BASE}${path}`, { ...init, headers })
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    const msg = body?.error ?? body?.message ?? `Request failed: ${res.status}`
    throw new Error(msg)
  }
  if (res.status === 204) return undefined as T
  return (await res.json()) as T
}

function unwrapData<T>(payload: unknown): T {
  if (!payload || typeof payload !== 'object') return payload as T
  const p = payload as Record<string, unknown>
  if ('data' in p) return p.data as T
  return payload as T
}

function normalizeAuthResponse(payload: unknown): AuthResponse {
  if (!payload || typeof payload !== 'object') throw new Error('Invalid auth response')
  const root = payload as Record<string, unknown>

  // Common API shapes: {token,...} | {data:{token,...}} | {auth:{token,...}}
  const candidates: Array<Record<string, unknown>> = [root]
  for (const k of ['data', 'auth', 'result', 'payload', 'response']) {
    const v = root[k]
    if (v && typeof v === 'object') candidates.push(v as Record<string, unknown>)
  }

  const readString = (o: Record<string, unknown>, key: string) => (typeof o[key] === 'string' ? (o[key] as string) : '')

  const tokenKeys = ['token', 'accessToken', 'access_token', 'jwt', 'idToken', 'authToken', 'auth_token']
  const idKeys = ['userId', 'user_id', 'id', 'sub']

  let token = ''
  let userId = ''
  let email = ''
  let name = ''

  for (const c of candidates) {
    if (!token) {
      for (const k of tokenKeys) {
        const v = readString(c, k)
        if (v) {
          token = v
          break
        }
      }
    }
    if (!userId) {
      for (const k of idKeys) {
        const v = readString(c, k)
        if (v) {
          userId = v
          break
        }
      }
    }
    if (!email) email = readString(c, 'email')
    if (!name) name = readString(c, 'name')
  }

  if (!token) throw new Error('Auth token missing in response')
  if (!userId) throw new Error('User id missing in response')

  return { token, userId, email, name }
}

export const api = {
  signup: (email: string, name: string, password: string) =>
    request<unknown>('/api/auth/signup', { method: 'POST', body: JSON.stringify({ email, name, password }) }).then(
      normalizeAuthResponse,
    ),
  login: (email: string, password: string) =>
    request<unknown>('/api/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }).then(normalizeAuthResponse),

  // New dashboard endpoints (recommended)
  dashboardOverview: () => request<unknown>('/api/dashboard/overview').then((p) => unwrapData<DashboardOverviewResponse>(p)),
  dashboardChart: (range: DashboardRange) =>
    request<unknown>(`/api/dashboard/task-done?range=${encodeURIComponent(range)}`).then((p) => unwrapData<unknown>(p)),

  // Legacy (if your backend already has it)
  dashboard: () => request<unknown>('/api/dashboard').then((p) => unwrapData<DashboardResponse>(p)),

  listProjects: () => request<unknown>('/api/projects').then((p) => unwrapData<ProjectResponse[]>(p)),
  createProject: (name: string, description?: string) =>
    request<unknown>('/api/projects', { method: 'POST', body: JSON.stringify({ name, description }) }).then((p) =>
      unwrapData<ProjectResponse>(p),
    ),
  getProject: (projectId: string, opts?: { taskPage?: number; taskSize?: number }) => {
    const taskPage = opts?.taskPage ?? 0
    const taskSize = opts?.taskSize ?? 20
    const qs = new URLSearchParams({ taskPage: String(taskPage), taskSize: String(taskSize) })
    return request<unknown>(`/api/projects/${projectId}?${qs.toString()}`).then((p) => unwrapData<ProjectDetailResponse>(p))
  },
  addMember: (projectId: string, email: string, role: ProjectRole) =>
    request<unknown>(`/api/projects/${projectId}/members`, {
      method: 'POST',
      body: JSON.stringify({ email, role }),
    }).then((p) => unwrapData<ProjectResponse>(p)),
  removeMember: (projectId: string, memberUserId: string) =>
    request<unknown>(`/api/projects/${projectId}/members/${memberUserId}`, { method: 'DELETE' }).then((p) =>
      unwrapData<ProjectResponse>(p),
    ),

  listTasks: (projectId: string) =>
    request<unknown>(`/api/projects/${projectId}/tasks`).then((p) => unwrapData<TaskResponse[]>(p)),
  createTask: (projectId: string, payload: { title: string; description?: string; assigneeEmail?: string; dueDate?: string }) =>
    request<unknown>(`/api/projects/${projectId}/tasks`, { method: 'POST', body: JSON.stringify(payload) }).then((p) =>
      unwrapData<TaskResponse>(p),
    ),
  updateTask: (
    projectId: string,
    taskId: string,
    payload: { title?: string; description?: string; assigneeEmail?: string; dueDate?: string },
  ) =>
    request<unknown>(`/api/projects/${projectId}/tasks/${taskId}`, { method: 'PUT', body: JSON.stringify(payload) }).then((p) =>
      unwrapData<TaskResponse>(p),
    ),
  updateTaskStatus: (projectId: string, taskId: string, status: TaskStatus) =>
    request<unknown>(`/api/projects/${projectId}/tasks/${taskId}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    }).then((p) => unwrapData<TaskResponse>(p)),
  deleteTask: (projectId: string, taskId: string) =>
    request<void>(`/api/projects/${projectId}/tasks/${taskId}`, { method: 'DELETE' }),

  getProfile: () =>
    request<unknown>('/api/profile').then((p) => unwrapData<ProfileResponse>(p)),
  updateProfile: (payload: { name: string; bio?: string }) =>
    request<unknown>('/api/profile', { method: 'PUT', body: JSON.stringify(payload) }).then((p) => unwrapData<ProfileResponse>(p)),
  changePassword: (currentPassword: string, newPassword: string) =>
    request<void>('/api/profile/password', { method: 'PATCH', body: JSON.stringify({ currentPassword, newPassword }) }),
}

