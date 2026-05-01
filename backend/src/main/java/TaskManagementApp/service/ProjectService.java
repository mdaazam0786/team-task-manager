package TaskManagementApp.service;

import TaskManagementApp.data.Project;
import TaskManagementApp.data.ProjectMember;
import TaskManagementApp.data.ProjectPermission;
import TaskManagementApp.data.ProjectRole;
import TaskManagementApp.data.User;
import TaskManagementApp.dto.AddProjectMemberRequest;
import TaskManagementApp.dto.CreateProjectRequest;
import TaskManagementApp.dto.ProjectDetailsResponse;
import TaskManagementApp.dto.ProjectMemberResponse;
import TaskManagementApp.dto.ProjectResponse;
import TaskManagementApp.dto.TaskResponse;
import TaskManagementApp.exception.BadRequestException;
import TaskManagementApp.exception.NotFoundException;
import TaskManagementApp.repository.ProjectRepository;
import TaskManagementApp.repository.TaskRepository;
import TaskManagementApp.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class ProjectService {
	private final ProjectRepository projectRepository;
	private final TaskRepository taskRepository;
	private final UserRepository userRepository;
	private final ProjectAccessService access;

	public ProjectService(
			ProjectRepository projectRepository,
			TaskRepository taskRepository,
			UserRepository userRepository,
			ProjectAccessService access
	) {
		this.projectRepository = projectRepository;
		this.taskRepository = taskRepository;
		this.userRepository = userRepository;
		this.access = access;
	}

	public ProjectResponse create(String creatorUserId, CreateProjectRequest req) {
		Project project = Project.builder()
				.name(req.name())
				.description(req.description())
				.members(List.of(ProjectMember.builder().userId(creatorUserId).role(ProjectRole.ADMIN).build()))
				.createdAt(Instant.now())
				.build();
		project = projectRepository.save(project);
		return toResponse(project);
	}

	public Page<ProjectResponse> listForUser(String userId, Pageable pageable) {
		return projectRepository.findAllForMember(userId, pageable).map(this::toResponse);
	}

	public ProjectResponse get(String projectId, String userId) {
		return toResponse(access.requireMemberProject(projectId, userId));
	}

	public ProjectDetailsResponse getDetails(String projectId, String userId, int taskPage, int taskSize) {
		var project = access.requirePermissionProject(projectId, userId, ProjectPermission.VIEW_PROJECT);
		var projectResponse = toResponse(project);

		// Build a userId -> name map from the already-resolved members
		Map<String, String> userNameById = projectResponse.members().stream()
				.filter(m -> m.userId() != null && m.name() != null)
				.collect(Collectors.toMap(ProjectMemberResponse::userId, ProjectMemberResponse::name));

		List<TaskResponse> taskList = taskRepository
				.findByProjectId(projectId, PageRequest.of(taskPage, taskSize))
				.map(task -> TaskService.toResponse(task, userNameById.get(task.getAssigneeId())))
				.getContent();

		long totalCount = taskRepository.countByProjectId(projectId);
		int totalPages = (int) Math.ceil((double) totalCount / taskSize);

		return new ProjectDetailsResponse(
				projectResponse.id(),
				projectResponse.name(),
				projectResponse.description(),
				projectResponse.members(),
				projectResponse.createdAt(),
				taskList,
				totalCount,
				totalPages,
				taskPage,
				taskSize
		);
	}

	public ProjectResponse addMember(String projectId, String adminUserId, AddProjectMemberRequest req) {
		Project project = access.requirePermissionProject(projectId, adminUserId, ProjectPermission.MANAGE_PROJECT_ACCESS);
		User user = userRepository.findByEmail(req.email().toLowerCase())
				.orElseThrow(() -> new NotFoundException("User not found"));

		boolean exists = project.getMembers().stream().anyMatch(m -> m.getUserId().equals(user.getId()));
		if (exists) {
			throw new BadRequestException("User is already a project member");
		}

		project.getMembers().add(ProjectMember.builder().userId(user.getId()).role(req.role()).build());
		project = projectRepository.save(project);
		return toResponse(project);
	}

	public ProjectResponse removeMember(String projectId, String adminUserId, String memberUserId) {
		Project project = access.requirePermissionProject(projectId, adminUserId, ProjectPermission.MANAGE_PROJECT_ACCESS);

		if (adminUserId.equals(memberUserId)) {
			throw new BadRequestException("Admin cannot remove themselves");
		}

		boolean removed = project.getMembers().removeIf(m -> m.getUserId().equals(memberUserId));
		if (!removed) {
			throw new NotFoundException("Member not found");
		}

		project = projectRepository.save(project);
		return toResponse(project);
	}

	private ProjectResponse toResponse(Project project) {
		Map<String, User> usersById = userRepository.findAllById(
				project.getMembers().stream().map(ProjectMember::getUserId).toList()
		).stream().collect(Collectors.toMap(User::getId, Function.identity()));

		List<ProjectMemberResponse> members = project.getMembers().stream().map(m -> {
			User u = usersById.get(m.getUserId());
			return new ProjectMemberResponse(
					m.getUserId(),
					u == null ? null : u.getEmail(),
					u == null ? null : u.getName(),
					m.getRole()
			);
		}).toList();

		return new ProjectResponse(project.getId(), project.getName(), project.getDescription(), members, project.getCreatedAt());
	}
}

