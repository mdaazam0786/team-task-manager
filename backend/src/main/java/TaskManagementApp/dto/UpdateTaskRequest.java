package TaskManagementApp.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateTaskRequest(
		@Size(min = 2, max = 200) String title,
		@Size(max = 5000) String description,
		String assigneeId,
		LocalDate dueDate
) {}

