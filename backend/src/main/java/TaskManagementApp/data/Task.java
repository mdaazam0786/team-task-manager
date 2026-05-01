package TaskManagementApp.data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("tasks")
public class Task {
	@Id
	private String id;

	@NotBlank
	@Indexed
	@Field("projectId")
	private String projectId;

	@NotBlank
	@Field("title")
	private String title;

	@Field("description")
	private String description;

	@NotNull
	@Field("status")
	private TaskStatus status;

	@Indexed
	@Field("assigneeId")
	private String assigneeId;

	@Field("dueDate")
	private LocalDate dueDate;

	@NotBlank
	@Field("createdBy")
	private String createdBy;

	@Field("createdAt")
	private Instant createdAt;

	@Field("updatedAt")
	private Instant updatedAt;
}

