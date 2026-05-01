package TaskManagementApp.repository;

import TaskManagementApp.data.Task;
import TaskManagementApp.data.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface TaskRepository extends MongoRepository<Task, String> {
	Page<Task> findByProjectId(String projectId, Pageable pageable);

	List<Task> findByAssigneeId(String assigneeId);

	long countByAssigneeIdAndStatus(String assigneeId, TaskStatus status);

	long countByAssigneeIdAndStatusAndUpdatedAtBetween(String assigneeId, TaskStatus status, Instant from, Instant to);

	long countByCreatedByAndCreatedAtBetween(String createdBy, Instant from, Instant to);

	long countByProjectId(String projectId);

	long countByProjectIdAndStatusNot(String projectId, TaskStatus status);

	@Query("{ 'assigneeId': ?0, 'dueDate': { $lt: ?1 }, 'status': { $ne: 'DONE' } }")
	List<Task> findOverdueForAssignee(String assigneeId, LocalDate beforeDate);
}

