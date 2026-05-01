package TaskManagementApp.dto;

import java.time.Instant;
import java.util.List;

public record ProjectDetailsResponse(
		String id,
		String name,
		String description,
		List<ProjectMemberResponse> members,
		Instant createdAt,
		List<TaskResponse> tasks,
		long taskTotalCount,
		int taskTotalPages,
		int taskCurrentPage,
		int taskPageSize
) {}

