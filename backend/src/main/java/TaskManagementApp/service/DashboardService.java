package TaskManagementApp.service;

import TaskManagementApp.data.Project;
import TaskManagementApp.data.TaskStatus;
import TaskManagementApp.dto.DashboardResponse;
import TaskManagementApp.dto.DashboardOverviewResponse;
import TaskManagementApp.dto.DashboardTaskDoneResponse;
import TaskManagementApp.exception.BadRequestException;
import TaskManagementApp.repository.ProjectRepository;
import TaskManagementApp.repository.TaskRepository;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
		long tasksDone = taskRepository.countByAssigneeIdAndStatus(userId, TaskStatus.DONE);
		long tasksInProgress = taskRepository.countByAssigneeIdAndStatus(userId, TaskStatus.IN_PROGRESS);

		long newTasks = taskRepository.countByAssigneeIdAndStatus(userId, TaskStatus.TODO);

		List<Project> projects = projectRepository.findAllForMember(
				userId,
				org.springframework.data.domain.Pageable.unpaged()
		).getContent();

		long projectsDone = projects.stream().filter(p -> {
			long total = taskRepository.countByProjectId(p.getId());
			if (total == 0) return false;
			long notDone = taskRepository.countByProjectIdAndStatusNot(p.getId(), TaskStatus.DONE);
			return notDone == 0;
		}).count();

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

	private DashboardTaskDoneResponse tasksCreatedDaily(String userId) {
		ZoneId zone = ZoneId.systemDefault();
		ZonedDateTime end = ZonedDateTime.now(zone);
		ZonedDateTime start = end.minusHours(24);
		List<DashboardTaskDoneResponse.Point> points = new ArrayList<>();
		for (int i = 0; i < 24; i++) {
			ZonedDateTime bucketStart = start.plusHours(i);
			ZonedDateTime bucketEnd = bucketStart.plusHours(1).minusNanos(1);
			long count = taskRepository.countByCreatedByAndCreatedAtBetween(
					userId,
					bucketStart.toInstant(),
					bucketEnd.toInstant()
			);
			String label = String.format("%02d:00", bucketStart.getHour());
			points.add(new DashboardTaskDoneResponse.Point(label, count));
		}
		return new DashboardTaskDoneResponse("daily", points);
	}

	private DashboardTaskDoneResponse tasksCreatedWeekly(String userId) {
		ZoneId zone = ZoneId.systemDefault();
		List<DashboardTaskDoneResponse.Point> points = new ArrayList<>();
		ZonedDateTime weekStart = ZonedDateTime.now(zone).with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay(zone);
		for (int i = 0; i < 7; i++) {
			ZonedDateTime dayStart = weekStart.plusDays(i);
			ZonedDateTime dayEnd = dayStart.plusDays(1).minusNanos(1);
			long count = taskRepository.countByCreatedByAndCreatedAtBetween(
					userId,
					dayStart.toInstant(),
					dayEnd.toInstant()
			);
			String label = dayStart.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
			points.add(new DashboardTaskDoneResponse.Point(label, count));
		}
		return new DashboardTaskDoneResponse("weekly", points);
	}

	private DashboardTaskDoneResponse tasksCreatedMonthly(String userId) {
		ZoneId zone = ZoneId.systemDefault();
		YearMonth ym = YearMonth.now(zone);
		ZonedDateTime monthStart = ym.atDay(1).atStartOfDay(zone);
		int days = ym.lengthOfMonth();
		List<DashboardTaskDoneResponse.Point> points = new ArrayList<>();
		for (int i = 0; i < days; i++) {
			ZonedDateTime dayStart = monthStart.plusDays(i);
			ZonedDateTime dayEnd = dayStart.plusDays(1).minusNanos(1);
			long count = taskRepository.countByCreatedByAndCreatedAtBetween(
					userId,
					dayStart.toInstant(),
					dayEnd.toInstant()
			);
			String label = String.valueOf(dayStart.getDayOfMonth());
			points.add(new DashboardTaskDoneResponse.Point(label, count));
		}
		return new DashboardTaskDoneResponse("monthly", points);
	}
}

