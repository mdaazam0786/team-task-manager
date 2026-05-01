package TaskManagementApp.service;

import TaskManagementApp.data.*;
import TaskManagementApp.dto.CreateTaskRequest;
import TaskManagementApp.dto.TaskResponse;
import TaskManagementApp.dto.UpdateTaskRequest;
import TaskManagementApp.exception.BadRequestException;
import TaskManagementApp.exception.ForbiddenException;
import TaskManagementApp.exception.NotFoundException;
import TaskManagementApp.repository.TaskRepository;
import TaskManagementApp.repository.UserRepository;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class TaskService {
	private final TaskRepository taskRepository;
	private final ProjectAccessService access;
	private final UserRepository userRepository;

	public TaskService(TaskRepository taskRepository, ProjectAccessService access, UserRepository userRepository) {
		this.taskRepository = taskRepository;
		this.access = access;
		this.userRepository = userRepository;
	}

	public Page<TaskResponse> listForProject(String projectId, String userId, Pageable pageable) {
		access.requirePermissionProject(projectId, userId, ProjectPermission.VIEW_PROJECT);
		return taskRepository.findByProjectId(projectId, pageable).map(this::toResponse);
	}

	public TaskResponse create(String projectId, String userId, CreateTaskRequest req) {
		Project project = access.requirePermissionProject(projectId, userId, ProjectPermission.MANAGE_TASKS);
		String assigneeId = resolveAssigneeId(project, req.assigneeEmail());

		// Default assignee to the creator so dashboard counts always reflect the task
		if (assigneeId == null) assigneeId = userId;

		Instant now = Instant.now();
		Task task = Task.builder()
				.projectId(projectId)
				.title(req.title())
				.description(req.description())
				.status(TaskStatus.TODO)
				.assigneeId(assigneeId)
				.dueDate(req.dueDate())
				.createdBy(userId)
				.createdAt(now)
				.updatedAt(now)
				.build();
		task = taskRepository.save(task);
		return toResponse(task);
	}

	public TaskResponse update(String projectId, String taskId, String userId, UpdateTaskRequest req) {
		Project project = access.requirePermissionProject(projectId, userId, ProjectPermission.MANAGE_TASKS);
		Task task = requireTaskInProject(taskId, projectId);

		if (req.assigneeEmail() != null) {
			String assigneeId = resolveAssigneeId(project, req.assigneeEmail());
			task.setAssigneeId(assigneeId);
		}
		if (req.title() != null) task.setTitle(req.title());
		if (req.description() != null) task.setDescription(req.description());
		if (req.dueDate() != null) task.setDueDate(req.dueDate());

		task.setUpdatedAt(Instant.now());
		task = taskRepository.save(task);
		return toResponse(task);
	}

	public void delete(String projectId, String taskId, String userId) {
		access.requirePermissionProject(projectId, userId, ProjectPermission.DELETE_TASKS);
		Task task = requireTaskInProject(taskId, projectId);
		taskRepository.delete(task);
	}

	public TaskResponse updateStatus(String projectId, String taskId, String userId, TaskStatus status) {
		Project project = access.requirePermissionProject(projectId, userId, ProjectPermission.MANAGE_TASKS);
		Task task = requireTaskInProject(taskId, projectId);

		boolean isAdmin = access.getRole(project, userId)
				.map(r -> r == ProjectRole.ADMIN)
				.orElse(false);

		boolean isAssignee = userId.equals(task.getAssigneeId());
		if (!isAdmin && !isAssignee) {
			throw new ForbiddenException("Only admin or assignee can update status");
		}

		task.setStatus(status);
		task.setUpdatedAt(Instant.now());
		task = taskRepository.save(task);
		return toResponse(task);
	}

	private Task requireTaskInProject(String taskId, String projectId) {
		Task task = taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));
		if (!projectId.equals(task.getProjectId())) {
			throw new NotFoundException("Task not found");
		}
		return task;
	}

	public TaskResponse toResponse(Task task) {
		String assigneeName = null;
		if (task.getAssigneeId() != null) {
			assigneeName = userRepository.findById(task.getAssigneeId())
					.map(User::getName)
					.orElse(null);
		}
		return toResponse(task, assigneeName);
	}

	public static TaskResponse toResponse(Task task, String assigneeName) {
		return new TaskResponse(
				task.getId(),
				task.getProjectId(),
				task.getTitle(),
				task.getDescription(),
				task.getStatus(),
				task.getAssigneeId(),
				assigneeName,
				task.getDueDate(),
				task.getCreatedBy(),
				task.getCreatedAt(),
				task.getUpdatedAt()
		);
	}

	private String resolveAssigneeId(Project project, String assigneeEmail) {
		if (assigneeEmail == null || assigneeEmail.isBlank()) return null;
		var user = userRepository.findByEmail(assigneeEmail.toLowerCase())
				.orElseThrow(() -> new NotFoundException("Assignee user not found with email: " + assigneeEmail));
		boolean isMember = project.getMembers().stream()
				.map(ProjectMember::getUserId)
				.anyMatch(user.getId()::equals);
		if (!isMember) {
			throw new BadRequestException("Assignee must be a project member");
		}
		return user.getId();
	}
}

