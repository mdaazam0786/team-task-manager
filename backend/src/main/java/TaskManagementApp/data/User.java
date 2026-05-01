package TaskManagementApp.data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
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
@Document("users")
public class User {
	@Id
	private String id;

	@NotBlank
	@Email
	@Indexed(unique = true)
	@Field("email")
	private String email;

	@NotBlank
	@Field("name")
	private String name;

	@Field("bio")
	private String bio;

	@NotBlank
	@Field("passwordHash")
	private String passwordHash;

	@Field("createdAt")
	private Instant createdAt;
}

