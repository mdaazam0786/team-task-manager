package TaskManagementApp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateTaskRequest(
		@NotBlank @Size(min = 2, max = 200) String title,
		@Size(max = 5000) String description,
		@Email String assigneeEmail,
		LocalDate dueDate
) {}

