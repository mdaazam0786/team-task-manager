package TaskManagementApp.repository;

import TaskManagementApp.data.Task;
import TaskManagementApp.data.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface TaskRepository extends MongoRepository<Task, String> {
	Page<Task> findByProjectId(String projectId, Pageable pageable);

	List<Task> findByAssigneeId(String assigneeId);

	long countByAssigneeIdAndStatus(String assigneeId, TaskStatus status);

	long countByAssigneeIdAndStatusAndUpdatedAtBetween(String assigneeId, TaskStatus status, Instant from, Instant to);

	long countByProjectId(String projectId);

	long countByProjectIdAndStatusNot(String projectId, TaskStatus status);

	@Query("{ 'assigneeId': ?0, 'dueDate': { $lt: ?1 }, 'status': { $ne: 'DONE' } }")
	List<Task> findOverdueForAssignee(String assigneeId, LocalDate beforeDate);

	// Fetch tasks in a time window — used by dashboard chart (replaces per-bucket count queries)
	@Query("{ 'createdBy': ?0, 'createdAt': { $gte: ?1, $lte: ?2 } }")
	List<Task> findCreatedByBetween(String createdBy, Instant from, Instant to);

	// Fetch tasks for multiple projects — used by projectsDone calculation
	@Query("{ 'projectId': { $in: ?0 } }")
	List<Task> findByProjectIdIn(List<String> projectIds);

	// Helper: group task counts by (projectId, status) in memory after a single query
	default Map<String, Map<TaskStatus, Long>> countByProjectIdsGroupedByStatus(List<String> projectIds) {
		return findByProjectIdIn(projectIds).stream()
				.collect(Collectors.groupingBy(
						Task::getProjectId,
						Collectors.groupingBy(Task::getStatus, Collectors.counting())
				));
	}
}
