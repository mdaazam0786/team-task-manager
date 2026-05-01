package TaskManagementApp.repository;

import TaskManagementApp.data.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface ProjectRepository extends MongoRepository<Project, String> {
	@Query("{ 'members.userId': ?0 }")
	Page<Project> findAllForMember(String userId, Pageable pageable);
}

