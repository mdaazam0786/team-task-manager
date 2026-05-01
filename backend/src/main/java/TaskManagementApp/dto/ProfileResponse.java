package TaskManagementApp.dto;

import java.time.Instant;

public record ProfileResponse(
		String id,
		String name,
		String email,
		String bio,
		Instant createdAt
) {}
