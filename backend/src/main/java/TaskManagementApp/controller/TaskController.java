package TaskManagementApp.controller;

import TaskManagementApp.dto.CreateTaskRequest;
import TaskManagementApp.dto.TaskResponse;
import TaskManagementApp.dto.UIBean;
import TaskManagementApp.dto.UIBeanPaginated;
import TaskManagementApp.dto.UpdateTaskRequest;
import TaskManagementApp.dto.UpdateTaskStatusRequest;
import TaskManagementApp.security.AuthContext;
import TaskManagementApp.service.TaskService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
public class TaskController {
	private final TaskService taskService;

	public TaskController(TaskService taskService) {
		this.taskService = taskService;
	}

	@GetMapping
	public ResponseEntity<UIBeanPaginated<List<TaskResponse>>> list(
			@PathVariable String projectId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size
	) {
		Page<TaskResponse> result = taskService.listForProject(
				projectId,
				AuthContext.requireUserId(),
				PageRequest.of(page, size)
		);
		UIBeanPaginated<List<TaskResponse>> response = UIBeanPaginated.success(
				result.getContent(),
				result.getTotalElements(),
				result.getTotalPages(),
				result.getNumber(),
				result.getSize()
		);
		return ResponseEntity.ok(response);
	}

	@PostMapping
	public ResponseEntity<UIBean<TaskResponse>> create(
			@PathVariable String projectId,
			@Valid @RequestBody CreateTaskRequest req
	) {
		TaskResponse task = taskService.create(projectId, AuthContext.requireUserId(), req);
		return new ResponseEntity<>(UIBean.success(task, "Task created successfully"), HttpStatus.CREATED);
	}

	@PutMapping("/{taskId}")
	public ResponseEntity<UIBean<TaskResponse>> update(
			@PathVariable String projectId,
			@PathVariable String taskId,
			@Valid @RequestBody UpdateTaskRequest req
	) {
		TaskResponse task = taskService.update(projectId, taskId, AuthContext.requireUserId(), req);
		return ResponseEntity.ok(UIBean.success(task, "Task updated successfully"));
	}

	@PatchMapping("/{taskId}/status")
	public ResponseEntity<UIBean<TaskResponse>> updateStatus(
			@PathVariable String projectId,
			@PathVariable String taskId,
			@Valid @RequestBody UpdateTaskStatusRequest req
	) {
		TaskResponse task = taskService.updateStatus(projectId, taskId, AuthContext.requireUserId(), req.status());
		return ResponseEntity.ok(UIBean.success(task, "Task status updated successfully"));
	}

	@DeleteMapping("/{taskId}")
	public ResponseEntity<UIBean<Void>> delete(@PathVariable String projectId, @PathVariable String taskId) {
		taskService.delete(projectId, taskId, AuthContext.requireUserId());
		return ResponseEntity.ok(UIBean.success(null, "Task deleted successfully"));
	}
}

