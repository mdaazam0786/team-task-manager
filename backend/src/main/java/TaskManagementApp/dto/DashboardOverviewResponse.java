package TaskManagementApp.dto;

public record DashboardOverviewResponse(
		long tasksDone,
		long tasksInProgress,
		long projectsDone,
		long newTasks
) {}

