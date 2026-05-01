package TaskManagementApp.dto;

public record DashboardResponse(
		long todo,
		long inProgress,
		long done,
		long overdue
) {}

