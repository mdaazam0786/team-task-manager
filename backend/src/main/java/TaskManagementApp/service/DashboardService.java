package TaskManagementApp.service;

import TaskManagementApp.data.TaskStatus;
import TaskManagementApp.dto.DashboardResponse;
import TaskManagementApp.dto.DashboardOverviewResponse;
import TaskManagementApp.dto.DashboardTaskDoneResponse;
import TaskManagementApp.exception.BadRequestException;
import TaskManagementApp.repository.ProjectRepository;
import TaskManagementApp.repository.TaskRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
	private final TaskRepository taskRepository;
	private final ProjectRepository projectRepository;

	public DashboardService(TaskRepository taskRepository, ProjectRepository projectRepository) {
		this.taskRepository = taskRepository;
		this.projectRepository = projectRepository;
	}

	public DashboardResponse getForUser(String userId) {
		long todo = taskRepository.countByAssigneeIdAndStatus(userId, TaskStatus.TODO);
		long inProgress = taskRepository.countByAssigneeIdAndStatus(userId, TaskStatus.IN_PROGRESS);
		long done = taskRepository.countByAssigneeIdAndStatus(userId, TaskStatus.DONE);
		long overdue = taskRepository.findOverdueForAssignee(userId, LocalDate.now()).size();
		return new DashboardResponse(todo, inProgress, done, overdue);
	}

	public DashboardOverviewResponse getOverviewForUser(String userId) {
		// 3 fast index-covered counts instead of N+1 project loop
		long tasksDone = taskRepository.countByAssigneeIdAndStatus(userId, TaskStatus.DONE);
		long tasksInProgress = taskRepository.countByAssigneeIdAndStatus(userId, TaskStatus.IN_PROGRESS);
		long newTasks = taskRepository.countByAssigneeIdAndStatus(userId, TaskStatus.TODO);

		// projectsDone: fetch all project IDs for user in one query,
		// then use a single aggregation to count per-project task statuses
		List<String> projectIds = projectRepository
				.findAllForMember(userId, Pageable.unpaged())
				.stream()
				.map(p -> p.getId())
				.collect(Collectors.toList());

		long projectsDone = 0;
		if (!projectIds.isEmpty()) {
			// One query: count tasks grouped by (projectId, status)
			Map<String, Map<TaskStatus, Long>> counts = taskRepository.countByProjectIdsGroupedByStatus(projectIds);
			for (String pid : projectIds) {
				Map<TaskStatus, Long> statusMap = counts.getOrDefault(pid, Map.of());
				long total = statusMap.values().stream().mapToLong(Long::longValue).sum();
				if (total == 0) continue;
				long notDone = statusMap.entrySet().stream()
						.filter(e -> e.getKey() != TaskStatus.DONE)
						.mapToLong(Map.Entry::getValue)
						.sum();
				if (notDone == 0) projectsDone++;
			}
		}

		return new DashboardOverviewResponse(tasksDone, tasksInProgress, projectsDone, newTasks);
	}

	public DashboardTaskDoneResponse getTaskDoneForUser(String userId, String range) {
		String normalized = range == null ? "" : range.trim().toLowerCase(Locale.ROOT);
		return switch (normalized) {
			case "daily" -> tasksCreatedDaily(userId);
			case "weekly" -> tasksCreatedWeekly(userId);
			case "monthly" -> tasksCreatedMonthly(userId);
			default -> throw new BadRequestException("Invalid range. Use daily|weekly|monthly");
		};
	}

	// Fetch all tasks in range once, then bucket in memory — replaces 24 DB round-trips
	private DashboardTaskDoneResponse tasksCreatedDaily(String userId) {
		ZoneId zone = ZoneId.systemDefault();
		ZonedDateTime end = ZonedDateTime.now(zone);
		ZonedDateTime start = end.minusHours(24);

		Map<Integer, Long> byHour = taskRepository
				.findCreatedByBetween(userId, start.toInstant(), end.toInstant())
				.stream()
				.collect(Collectors.groupingBy(
						t -> t.getCreatedAt().atZone(zone).getHour(),
						Collectors.counting()
				));

		List<DashboardTaskDoneResponse.Point> points = new ArrayList<>();
		for (int i = 0; i < 24; i++) {
			ZonedDateTime bucket = start.plusHours(i);
			String label = String.format("%02d:00", bucket.getHour());
			points.add(new DashboardTaskDoneResponse.Point(label, byHour.getOrDefault(bucket.getHour(), 0L)));
		}
		return new DashboardTaskDoneResponse("daily", points);
	}

	// Fetch all tasks in week once, then bucket in memory — replaces 7 DB round-trips
	private DashboardTaskDoneResponse tasksCreatedWeekly(String userId) {
		ZoneId zone = ZoneId.systemDefault();
		ZonedDateTime weekStart = ZonedDateTime.now(zone).with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay(zone);
		ZonedDateTime weekEnd = weekStart.plusDays(7);

		Map<Integer, Long> byDow = taskRepository
				.findCreatedByBetween(userId, weekStart.toInstant(), weekEnd.toInstant())
				.stream()
				.collect(Collectors.groupingBy(
						t -> t.getCreatedAt().atZone(zone).getDayOfWeek().getValue(), // 1=Mon … 7=Sun
						Collectors.counting()
				));

		List<DashboardTaskDoneResponse.Point> points = new ArrayList<>();
		for (int i = 0; i < 7; i++) {
			ZonedDateTime day = weekStart.plusDays(i);
			String label = day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
			points.add(new DashboardTaskDoneResponse.Point(label, byDow.getOrDefault(day.getDayOfWeek().getValue(), 0L)));
		}
		return new DashboardTaskDoneResponse("weekly", points);
	}

	// Fetch all tasks in month once, then bucket in memory — replaces up to 31 DB round-trips
	private DashboardTaskDoneResponse tasksCreatedMonthly(String userId) {
		ZoneId zone = ZoneId.systemDefault();
		YearMonth ym = YearMonth.now(zone);
		ZonedDateTime monthStart = ym.atDay(1).atStartOfDay(zone);
		ZonedDateTime monthEnd = monthStart.plusMonths(1);

		Map<Integer, Long> byDay = taskRepository
				.findCreatedByBetween(userId, monthStart.toInstant(), monthEnd.toInstant())
				.stream()
				.collect(Collectors.groupingBy(
						t -> t.getCreatedAt().atZone(zone).getDayOfMonth(),
						Collectors.counting()
				));

		List<DashboardTaskDoneResponse.Point> points = new ArrayList<>();
		for (int i = 1; i <= ym.lengthOfMonth(); i++) {
			points.add(new DashboardTaskDoneResponse.Point(String.valueOf(i), byDay.getOrDefault(i, 0L)));
		}
		return new DashboardTaskDoneResponse("monthly", points);
	}
}
