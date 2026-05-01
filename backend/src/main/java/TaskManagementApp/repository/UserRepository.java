package TaskManagementApp.repository;

import TaskManagementApp.data.User;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);
}

