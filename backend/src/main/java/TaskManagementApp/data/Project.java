package TaskManagementApp.data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("projects")
public class Project {
	@Id
	private String id;

	@NotBlank
	@Field("name")
	private String name;

	@Field("description")
	private String description;

	@NotEmpty
	@Valid
	@Builder.Default
	@Field("members")
	private List<ProjectMember> members = new ArrayList<>();

	@Field("createdAt")
	private Instant createdAt;
}

