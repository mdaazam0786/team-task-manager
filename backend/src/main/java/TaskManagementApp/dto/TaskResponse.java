package TaskManagementApp.dto;

import TaskManagementApp.data.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;

public record TaskResponse(
		String id,
		String projectId,
		String title,
		String description,
		TaskStatus status,
		String assigneeId,
		String assigneeName,
		LocalDate dueDate,
		String createdBy,
		Instant createdAt,
		Instant updatedAt
) {}

