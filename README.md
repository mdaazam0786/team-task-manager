# Team Task Manager

A full-stack collaborative task management app where teams can create projects, manage tasks, and track progress — all secured with JWT-based authentication and role-based access control.

---

## Features

- **Authentication** — JWT-based signup and login with secure token storage
- **Project Management** — Create projects, view your project list with pagination
- **Team Collaboration** — Add or remove project members by email with role assignment (Admin / Member)
- **Task Tracking** — Create, edit, delete, and move tasks through `TODO → IN_PROGRESS → DONE` statuses
- **Task Assignment** — Assign tasks to project members; only the admin or the assignee can update a task's status
- **Due Dates & Overdue Detection** — Set due dates on tasks; overdue tasks are highlighted in the UI with a warning badge
- **Dashboard** — Personal KPIs (tasks completed, in progress, new, overdue, projects done) with a task-creation chart
- **Chart Ranges** — Dashboard chart supports daily (hourly), weekly (day of week), and monthly (day of month) views
- **RBAC** — Fine-grained permissions: `VIEW_PROJECT`, `MANAGE_PROJECT_ACCESS`, `MANAGE_TASKS`, `DELETE_TASKS`
- **Paginated APIs** — All list endpoints support `page` and `size` query params

---

## Tech Stack

| Layer      | Technology                                      |
|------------|-------------------------------------------------|
| Frontend   | React 19, TypeScript, Vite, React Router v7     |
| Backend    | Spring Boot 3.5, Java 17, Spring Security       |
| Database   | MongoDB (local or Atlas)                        |
| Auth       | JWT (jjwt 0.12), BCrypt password hashing        |
| Build      | Maven (backend), npm (frontend)                 |
| Other      | Lombok, Bean Validation, CORS configuration     |

---

## Folder Structure

```
.
├── backend/                        # Spring Boot REST API
│   └── src/main/java/TaskManagementApp/
│       ├── config/                 # MongoDB configuration
│       ├── controller/             # REST controllers (Auth, Project, Task, Dashboard)
│       ├── data/                   # MongoDB document models + enums (Task, Project, User, TaskStatus, ProjectRole…)
│       ├── dto/                    # Request/response DTOs
│       ├── exception/              # Global exception handling
│       ├── repository/             # Spring Data MongoDB repositories
│       ├── security/               # JWT filter, service, security config
│       └── service/                # Business logic
│
└── frontend/                       # React + Vite SPA
    └── src/
        ├── pages/                  # Dashboard, Login, Signup, Projects, ProjectDetail
        ├── api.ts                  # Typed API client (fetch-based)
        ├── auth.tsx                # Auth provider
        ├── auth-context.ts         # Auth context definition
        └── App.tsx                 # Routes
```

---

## Setup Instructions

### Prerequisites

- Java 17+
- Node.js 18+
- MongoDB (local instance or [MongoDB Atlas](https://www.mongodb.com/atlas))

---

### Backend

1. Clone the repo and navigate to the backend:

   ```bash
   cd backend
   ```

2. Set environment variables (or let the defaults apply for local dev):

   | Variable               | Default                                             | Description                         |
   |------------------------|-----------------------------------------------------|-------------------------------------|
   | `MONGODB_URI`          | `mongodb://localhost:27017/`                        | MongoDB connection string           |
   | `MONGODB_DATABASE`     | `taskdb`                                            | Database name                       |
   | `JWT_SECRET`           | `dev-secret-change-me-dev-secret-change-me-32bytes` | HS256 signing secret (min 32 chars) |
   | `JWT_EXP_SECONDS`      | `604800` (7 days)                                   | Token expiry in seconds             |
   | `CORS_ALLOWED_ORIGINS` | `http://localhost:5173`                             | Allowed frontend origin             |
   | `MONGO_AUTO_INDEX`     | `false`                                             | Enable MongoDB index auto-creation  |

   > ⚠️ Always override `JWT_SECRET` with a strong random value in production.

3. Run the backend:

   ```bash
   ./mvnw spring-boot:run
   ```

   The API will be available at `http://localhost:8080`.

---

### Frontend

1. Navigate to the frontend directory:

   ```bash
   cd frontend
   ```

2. Install dependencies:

   ```bash
   npm install
   ```

3. Start the dev server:

   ```bash
   npm run dev
   ```

   The app will run at `http://localhost:5173`. All `/api/*` requests are proxied to the backend.

4. To build for production:

   ```bash
   npm run build
   ```

---

## API Endpoints

All endpoints (except auth) require an `Authorization: Bearer <token>` header.

### Auth — `/api/auth`

| Method | Endpoint           | Description         | Auth Required |
|--------|--------------------|---------------------|---------------|
| POST   | `/api/auth/signup` | Register a new user | No            |
| POST   | `/api/auth/login`  | Login, receive JWT  | No            |

### Projects — `/api/projects`

| Method | Endpoint                                     | Description                        |
|--------|----------------------------------------------|------------------------------------|
| POST   | `/api/projects`                              | Create a new project               |
| GET    | `/api/projects?page=0&size=20`               | List projects for the current user |
| GET    | `/api/projects/{projectId}`                  | Get project details + tasks        |
| POST   | `/api/projects/{projectId}/members`          | Add a member to a project (Admin)  |
| DELETE | `/api/projects/{projectId}/members/{userId}` | Remove a member from a project (Admin) |

### Tasks — `/api/projects/{projectId}/tasks`

| Method | Endpoint                                          | Description                                        |
|--------|---------------------------------------------------|----------------------------------------------------|
| GET    | `/api/projects/{projectId}/tasks?page=0&size=20`  | List tasks in a project                            |
| POST   | `/api/projects/{projectId}/tasks`                 | Create a task (with optional assigneeId, dueDate)  |
| PUT    | `/api/projects/{projectId}/tasks/{taskId}`        | Update a task                                      |
| PATCH  | `/api/projects/{projectId}/tasks/{taskId}/status` | Update task status (Admin or assignee only)        |
| DELETE | `/api/projects/{projectId}/tasks/{taskId}`        | Delete a task                                      |

### Dashboard — `/api/dashboard`

| Method | Endpoint                               | Description                                        |
|--------|----------------------------------------|----------------------------------------------------|
| GET    | `/api/dashboard`                       | Full dashboard data (todo, inProgress, done, overdue counts) |
| GET    | `/api/dashboard/overview`              | Summary KPIs (tasksDone, inProgress, projectsDone, newTasks) |
| GET    | `/api/dashboard/task-done?range=daily` | Task creation chart — `range`: `daily`, `weekly`, `monthly` |

---

## Deploying to Railway

The project deploys as two Railway services — **backend** and **frontend** — both using Docker. MongoDB Atlas is used as the database (Railway's own Mongo plugin works too).

### 1. Push your code to GitHub

Railway deploys from a Git repo, so make sure your code is pushed.

### 2. Create a Railway project

Go to [railway.app](https://railway.app), create a new project, and add two services.

---

### Service 1 — Backend (Spring Boot)

1. **New Service → GitHub Repo** → select your repo
2. Set **Root Directory** to `backend`
3. Railway will detect the `Dockerfile` automatically
4. Add these **environment variables** in the Railway dashboard:

   | Variable               | Value                                      |
   |------------------------|--------------------------------------------|
   | `MONGODB_URI`          | Your MongoDB Atlas connection string       |
   | `MONGODB_DATABASE`     | `taskdb` (or your preferred db name)       |
   | `JWT_SECRET`           | A strong random string (min 32 chars)      |
   | `CORS_ALLOWED_ORIGINS` | Your frontend Railway URL (set after step below) |

   > Railway injects `PORT` automatically — no need to set it.

5. Deploy. Once live, copy the generated domain (e.g. `https://backend-production-xxxx.up.railway.app`).

---

### Service 2 — Frontend (React + nginx)

1. **New Service → GitHub Repo** → same repo
2. Set **Root Directory** to `frontend`
3. Railway will detect the `Dockerfile` automatically
4. Add this **build argument / environment variable**:

   | Variable       | Value                                                  |
   |----------------|--------------------------------------------------------|
   | `VITE_API_URL` | Your backend Railway URL from the step above           |

   > In Railway, build args are set under **Variables** — the Dockerfile reads `VITE_API_URL` as an `ARG` at build time.

5. Deploy. Copy the frontend domain.

---

### 6. Wire CORS on the backend

Go back to the **backend service → Variables** and update:

```
CORS_ALLOWED_ORIGINS=https://your-frontend-xxxx.up.railway.app
```

Then redeploy the backend (Railway does this automatically on variable change).

---

### Summary of env vars

| Service  | Variable               | Example value                                      |
|----------|------------------------|----------------------------------------------------|
| Backend  | `MONGODB_URI`          | `mongodb+srv://user:pass@cluster.mongodb.net/`     |
| Backend  | `MONGODB_DATABASE`     | `taskdb`                                           |
| Backend  | `JWT_SECRET`           | `a-very-long-random-secret-string-here`            |
| Backend  | `CORS_ALLOWED_ORIGINS` | `https://frontend-xxxx.up.railway.app`             |
| Frontend | `VITE_API_URL`         | `https://backend-xxxx.up.railway.app`              |

---

## Future Improvements

- **Comments** — Per-task comment threads for team discussion
- **File attachments** — Attach files or images to tasks
- **Activity log** — Audit trail of changes per project
- **Search & filtering** — Filter tasks by status, assignee, or due date
- **OAuth2 login** — Sign in with Google / GitHub
- **Real-time updates** — WebSocket or SSE for live task board changes
- **Email notifications** — Notify assignees when a task is assigned or approaching its due date
- **Dark mode** — Theme toggle in the frontend
- **Mobile app** — React Native client using the same REST API

---

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Commit your changes: `git commit -m "feat: add your feature"`
4. Push to your branch: `git push origin feature/your-feature-name`
5. Open a Pull Request

Please follow [Conventional Commits](https://www.conventionalcommits.org/) for commit messages.

---

## License

This project is licensed under the [MIT License](LICENSE).
