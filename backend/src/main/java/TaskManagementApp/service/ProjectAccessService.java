package TaskManagementApp.service;

import TaskManagementApp.data.Project;
import TaskManagementApp.data.ProjectMember;
import TaskManagementApp.data.ProjectPermission;
import TaskManagementApp.data.ProjectRole;
import TaskManagementApp.exception.ForbiddenException;
import TaskManagementApp.exception.NotFoundException;
import TaskManagementApp.repository.ProjectRepository;
import java.util.EnumSet;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ProjectAccessService {
	private final ProjectRepository projectRepository;

	public ProjectAccessService(ProjectRepository projectRepository) {
		this.projectRepository = projectRepository;
	}

	public Project requireProject(String projectId) {
		return projectRepository.findById(projectId)
				.orElseThrow(() -> new NotFoundException("Project not found"));
	}

	public Project requireMemberProject(String projectId, String userId) {
		Project project = requireProject(projectId);
		if (getRole(project, userId).isEmpty()) {
			throw new ForbiddenException("Not a member of this project");
		}
		return project;
	}

	public Project requireAdminProject(String projectId, String userId) {
		Project project = requireMemberProject(projectId, userId);
		if (getRole(project, userId).orElseThrow() != ProjectRole.ADMIN) {
			throw new ForbiddenException("Admin role required");
		}
		return project;
	}

	public Project requirePermissionProject(String projectId, String userId, ProjectPermission permission) {
		Project project = requireMemberProject(projectId, userId);
		ProjectRole role = getRole(project, userId).orElseThrow();
		if (!permissionsFor(role).contains(permission)) {
			throw new ForbiddenException("Insufficient permissions");
		}
		return project;
	}

	public Optional<ProjectRole> getRole(Project project, String userId) {
		return project.getMembers().stream()
				.filter(m -> userId.equals(m.getUserId()))
				.map(ProjectMember::getRole)
				.findFirst();
	}

	private static EnumSet<ProjectPermission> permissionsFor(ProjectRole role) {
		return switch (role) {
			case ADMIN -> EnumSet.allOf(ProjectPermission.class);
			case MEMBER -> EnumSet.of(ProjectPermission.VIEW_PROJECT, ProjectPermission.MANAGE_TASKS, ProjectPermission.DELETE_TASKS);
		};
	}
}

