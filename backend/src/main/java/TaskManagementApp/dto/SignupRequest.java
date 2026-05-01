package TaskManagementApp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
		@NotBlank @Email String email,
		@NotBlank @Size(min = 2, max = 100) String name,
		@NotBlank @Size(min = 8, max = 200) String password
) {}
