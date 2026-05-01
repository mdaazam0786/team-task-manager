package TaskManagementApp.service;

import TaskManagementApp.data.Project;
import TaskManagementApp.data.ProjectMember;
import TaskManagementApp.data.ProjectPermission;
import TaskManagementApp.data.ProjectRole;
import TaskManagementApp.data.Task;
import TaskManagementApp.data.TaskStatus;
import TaskManagementApp.dto.CreateTaskRequest;
import TaskManagementApp.dto.TaskResponse;
import TaskManagementApp.dto.UpdateTaskRequest;
import TaskManagementApp.exception.BadRequestException;
import TaskManagementApp.exception.ForbiddenException;
import TaskManagementApp.exception.NotFoundException;
import TaskManagementApp.repository.TaskRepository;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class TaskService {
	private final TaskRepository taskRepository;
	private final ProjectAccessService access;

	public TaskService(TaskRepository taskRepository, ProjectAccessService access) {
		this.taskRepository = taskRepository;
		this.access = access;
	}

	public Page<TaskResponse> listForProject(String projectId, String userId, Pageable pageable) {
		access.requirePermissionProject(projectId, userId, ProjectPermission.VIEW_PROJECT);
		return taskRepository.findByProjectId(projectId, pageable).map(TaskService::toResponse);
	}

	public TaskResponse create(String projectId, String userId, CreateTaskRequest req) {
		Project project = access.requirePermissionProject(projectId, userId, ProjectPermission.MANAGE_TASKS);
		requireAssigneeMemberIfPresent(project, req.assigneeId());

		Instant now = Instant.now();
		Task task = Task.builder()
				.projectId(projectId)
				.title(req.title())
				.description(req.description())
				.status(TaskStatus.TODO)
				.assigneeId(req.assigneeId())
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

		if (req.assigneeId() != null) {
			requireAssigneeMemberIfPresent(project, req.assigneeId());
			task.setAssigneeId(req.assigneeId());
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

	public static TaskResponse toResponse(Task task) {
		return new TaskResponse(
				task.getId(),
				task.getProjectId(),
				task.getTitle(),
				task.getDescription(),
				task.getStatus(),
				task.getAssigneeId(),
				task.getDueDate(),
				task.getCreatedBy(),
				task.getCreatedAt(),
				task.getUpdatedAt()
		);
	}

	private static void requireAssigneeMemberIfPresent(Project project, String assigneeId) {
		if (assigneeId == null || assigneeId.isBlank()) return;
		boolean isMember = project.getMembers().stream().map(ProjectMember::getUserId).anyMatch(assigneeId::equals);
		if (!isMember) {
			throw new BadRequestException("Assignee must be a project member");
		}
	}
}

