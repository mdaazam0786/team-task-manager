package TaskManagementApp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateTaskRequest(
		@Size(min = 2, max = 200) String title,
		@Size(max = 5000) String description,
		@Email String assigneeEmail,
		LocalDate dueDate
) {}

