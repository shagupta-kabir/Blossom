package io.blossom.erp.service;

import io.blossom.erp.domain.Employee;
import io.blossom.erp.domain.Priority;
import io.blossom.erp.domain.Project;
import io.blossom.erp.domain.Role;
import io.blossom.erp.domain.Task;
import io.blossom.erp.domain.TaskStatus;
import io.blossom.erp.dto.WorkDtos.TaskRequest;
import io.blossom.erp.dto.WorkDtos.TaskResponse;
import io.blossom.erp.dto.WorkDtos.TaskStatusRequest;
import io.blossom.erp.exception.ResourceNotFoundException;
import io.blossom.erp.repository.EmployeeRepository;
import io.blossom.erp.repository.ProjectRepository;
import io.blossom.erp.repository.TaskRepository;
import io.blossom.erp.security.AuthUser;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TaskService {

    private final TaskRepository tasks;
    private final ProjectRepository projects;
    private final EmployeeRepository employees;

    public TaskService(TaskRepository tasks, ProjectRepository projects, EmployeeRepository employees) {
        this.tasks = tasks;
        this.projects = projects;
        this.employees = employees;
    }

    public List<TaskResponse> list(String projectId, String assigneeId, TaskStatus status, boolean mine, AuthUser user) {
        String assignee = mine ? user.employeeId() : assigneeId;
        if (mine && assignee == null) {
            return List.of();
        }
        List<Task> rows;
        if (StringUtils.hasText(projectId)) {
            rows = tasks.findByProjectIdOrderByCreatedAtDesc(projectId);
        } else if (StringUtils.hasText(assignee)) {
            rows = tasks.findByAssigneeIdOrderByCreatedAtDesc(assignee);
        } else {
            rows = tasks.findAllByOrderByCreatedAtDesc();
        }
        List<Task> filtered = rows.stream()
                .filter(t -> !StringUtils.hasText(assignee) || assignee.equals(t.getAssigneeId()))
                .filter(t -> status == null || status == t.getStatus())
                .toList();
        return toResponses(filtered);
    }

    public TaskResponse get(String id) {
        return toResponses(List.of(find(id))).get(0);
    }

    public TaskResponse create(TaskRequest req) {
        Task t = new Task();
        t.setCreatedAt(Instant.now());
        apply(t, req);
        return toResponses(List.of(tasks.save(t))).get(0);
    }

    public TaskResponse update(String id, TaskRequest req) {
        Task t = find(id);
        apply(t, req);
        return toResponses(List.of(tasks.save(t))).get(0);
    }

    /** Admins/managers can move any task; everybody else only the tasks assigned to them. */
    public TaskResponse updateStatus(String id, TaskStatusRequest req, AuthUser user) {
        Task t = find(id);
        boolean privileged = user.hasAnyRole(Role.ADMIN, Role.MANAGER);
        boolean assignee = user.employeeId() != null && user.employeeId().equals(t.getAssigneeId());
        if (!privileged && !assignee) {
            throw new AccessDeniedException("You can only update tasks assigned to you");
        }
        t.setStatus(req.status());
        return toResponses(List.of(tasks.save(t))).get(0);
    }

    public void delete(String id) {
        tasks.delete(find(id));
    }

    private void apply(Task t, TaskRequest req) {
        if (!projects.existsById(req.projectId())) {
            throw new ResourceNotFoundException("Project", req.projectId());
        }
        if (StringUtils.hasText(req.assigneeId()) && !employees.existsById(req.assigneeId())) {
            throw new ResourceNotFoundException("Employee (assignee)", req.assigneeId());
        }
        t.setProjectId(req.projectId());
        t.setTitle(req.title().trim());
        t.setDescription(req.description());
        t.setStatus(req.status() == null ? TaskStatus.TODO : req.status());
        t.setPriority(req.priority() == null ? Priority.MEDIUM : req.priority());
        t.setAssigneeId(StringUtils.hasText(req.assigneeId()) ? req.assigneeId() : null);
        t.setDueDate(req.dueDate());
    }

    private Task find(String id) {
        return tasks.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }

    private List<TaskResponse> toResponses(List<Task> rows) {
        Map<String, String> projectNames = projects.findAllById(rows.stream().map(Task::getProjectId).distinct().toList())
                .stream().collect(Collectors.toMap(Project::getId, Project::getName, (a, b) -> a));
        Map<String, String> people = employees.findAllById(rows.stream().map(Task::getAssigneeId)
                        .filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Employee::getId, e -> e.getFirstName() + " " + e.getLastName(), (a, b) -> a));
        LocalDate today = LocalDate.now();
        return rows.stream().map(t -> new TaskResponse(t.getId(), t.getProjectId(), projectNames.get(t.getProjectId()),
                t.getTitle(), t.getDescription(), t.getStatus(), t.getPriority(), t.getAssigneeId(),
                t.getAssigneeId() == null ? null : people.get(t.getAssigneeId()), t.getDueDate(),
                t.getDueDate() != null && t.getStatus() != TaskStatus.DONE && t.getDueDate().isBefore(today))).toList();
    }
}
