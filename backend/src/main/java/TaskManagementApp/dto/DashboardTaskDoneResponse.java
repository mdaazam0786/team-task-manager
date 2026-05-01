package TaskManagementApp.dto;

import java.util.List;

public record DashboardTaskDoneResponse(
		String range,
		List<Point> points
) {
	public record Point(String label, long value) {}
}

