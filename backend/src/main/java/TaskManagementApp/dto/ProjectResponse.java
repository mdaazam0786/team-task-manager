package TaskManagementApp.dto;

import java.time.Instant;
import java.util.List;

public record ProjectResponse(
		String id,
		String name,
		String description,
		List<ProjectMemberResponse> members,
		Instant createdAt
) {}

