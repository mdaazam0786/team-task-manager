package TaskManagementApp.controller;

import TaskManagementApp.dto.DashboardResponse;
import TaskManagementApp.dto.DashboardOverviewResponse;
import TaskManagementApp.dto.DashboardTaskDoneResponse;
import TaskManagementApp.dto.UIBean;
import TaskManagementApp.security.AuthContext;
import TaskManagementApp.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
	private final DashboardService dashboardService;

	public DashboardController(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}

	@GetMapping
	public ResponseEntity<UIBean<DashboardResponse>> get() {
		DashboardResponse dashboard = dashboardService.getForUser(AuthContext.requireUserId());
		return ResponseEntity.ok(UIBean.success(dashboard));
	}

	@GetMapping("/overview")
	public ResponseEntity<UIBean<DashboardOverviewResponse>> overview() {
		DashboardOverviewResponse overview = dashboardService.getOverviewForUser(AuthContext.requireUserId());
		return ResponseEntity.ok(UIBean.success(overview));
	}

	@GetMapping("/task-done")
	public ResponseEntity<UIBean<DashboardTaskDoneResponse>> taskDone(@RequestParam String range) {
		DashboardTaskDoneResponse chart = dashboardService.getTaskDoneForUser(AuthContext.requireUserId(), range);
		return ResponseEntity.ok(UIBean.success(chart));
	}
}

