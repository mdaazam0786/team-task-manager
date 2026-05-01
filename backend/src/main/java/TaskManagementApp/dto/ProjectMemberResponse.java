package TaskManagementApp.dto;


import TaskManagementApp.data.ProjectRole;

public record ProjectMemberResponse(
		String userId,
		String email,
		String name,
		ProjectRole role
) {}

