package TaskManagementApp.data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMember {
	@NotBlank
	@Field("userId")
	private String userId;

	@NotNull
	@Field("role")
	private ProjectRole role;
}

