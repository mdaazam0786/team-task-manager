package TaskManagementApp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
		@NotBlank @Size(min = 2, max = 100) String name,
		@Size(max = 300) String bio
) {}
