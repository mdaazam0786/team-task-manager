package TaskManagementApp.controller;

import TaskManagementApp.dto.AddProjectMemberRequest;
import TaskManagementApp.dto.CreateProjectRequest;
import TaskManagementApp.dto.ProjectDetailsResponse;
import TaskManagementApp.dto.ProjectResponse;
import TaskManagementApp.dto.UIBean;
import TaskManagementApp.dto.UIBeanPaginated;
import TaskManagementApp.security.AuthContext;
import TaskManagementApp.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
	private final ProjectService projectService;

	public ProjectController(ProjectService projectService) {
		this.projectService = projectService;
	}

	@PostMapping
	public ResponseEntity<UIBean<ProjectResponse>> create(@Valid @RequestBody CreateProjectRequest req) {
		ProjectResponse project = projectService.create(AuthContext.requireUserId(), req);
		return new ResponseEntity<>(UIBean.success(project, "Project created successfully"), HttpStatus.CREATED);
	}

	@GetMapping
	public ResponseEntity<UIBeanPaginated<List<ProjectResponse>>> listMine(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size
	) {
		Page<ProjectResponse> result = projectService.listForUser(AuthContext.requireUserId(), PageRequest.of(page, size));
		UIBeanPaginated<List<ProjectResponse>> response = UIBeanPaginated.success(
				result.getContent(),
				result.getTotalElements(),
				result.getTotalPages(),
				result.getNumber(),
				result.getSize()
		);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{projectId}")
	public ResponseEntity<UIBean<ProjectDetailsResponse>> get(
			@PathVariable String projectId,
			@RequestParam(defaultValue = "0") int taskPage,
			@RequestParam(defaultValue = "20") int taskSize
	) {
		ProjectDetailsResponse project = projectService.getDetails(projectId, AuthContext.requireUserId(), taskPage, taskSize);
		return ResponseEntity.ok(UIBean.success(project));
	}

	@PostMapping("/{projectId}/members")
	public ResponseEntity<UIBean<ProjectResponse>> addMember(
			@PathVariable String projectId,
			@Valid @RequestBody AddProjectMemberRequest req
	) {
		ProjectResponse project = projectService.addMember(projectId, AuthContext.requireUserId(), req);
		return ResponseEntity.ok(UIBean.success(project, "Member added successfully"));
	}

	@DeleteMapping("/{projectId}/members/{memberUserId}")
	public ResponseEntity<UIBean<ProjectResponse>> removeMember(
			@PathVariable String projectId,
			@PathVariable String memberUserId
	) {
		ProjectResponse project = projectService.removeMember(projectId, AuthContext.requireUserId(), memberUserId);
		return ResponseEntity.ok(UIBean.success(project, "Member removed successfully"));
	}
}

