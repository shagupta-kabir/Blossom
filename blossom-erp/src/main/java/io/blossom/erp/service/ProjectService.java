package io.blossom.erp.service;

import io.blossom.erp.domain.Project;
import io.blossom.erp.domain.ProjectStatus;
import io.blossom.erp.domain.TaskStatus;
import io.blossom.erp.dto.WorkDtos.ProjectRequest;
import io.blossom.erp.dto.WorkDtos.ProjectResponse;
import io.blossom.erp.exception.BadRequestException;
import io.blossom.erp.exception.ResourceNotFoundException;
import io.blossom.erp.repository.EmployeeRepository;
import io.blossom.erp.repository.ProjectRepository;
import io.blossom.erp.repository.TaskRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ProjectService {

    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final EmployeeRepository employees;

    public ProjectService(ProjectRepository projects, TaskRepository tasks, EmployeeRepository employees) {
        this.projects = projects;
        this.tasks = tasks;
        this.employees = employees;
    }

    public List<ProjectResponse> list() {
        return projects.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    public ProjectResponse get(String id) {
        return toResponse(find(id));
    }

    public ProjectResponse create(ProjectRequest req) {
        Project p = new Project();
        p.setCreatedAt(Instant.now());
        apply(p, req);
        return toResponse(projects.save(p));
    }

    public ProjectResponse update(String id, ProjectRequest req) {
        Project p = find(id);
        apply(p, req);
        return toResponse(projects.save(p));
    }

    public void delete(String id) {
        Project p = find(id);
        tasks.deleteByProjectId(id);
        projects.delete(p);
    }

    private void apply(Project p, ProjectRequest req) {
        if (req.startDate() != null && req.dueDate() != null && req.dueDate().isBefore(req.startDate())) {
            throw new BadRequestException("Due date must not be before start date");
        }
        if (StringUtils.hasText(req.ownerId()) && !employees.existsById(req.ownerId())) {
            throw new ResourceNotFoundException("Employee (owner)", req.ownerId());
        }
        p.setName(req.name().trim());
        p.setDescription(req.description());
        p.setStatus(req.status() == null ? ProjectStatus.PLANNED : req.status());
        p.setOwnerId(StringUtils.hasText(req.ownerId()) ? req.ownerId() : null);
        p.setStartDate(req.startDate());
        p.setDueDate(req.dueDate());
        p.setBudget(req.budget());
    }

    private Project find(String id) {
        return projects.findById(id).orElseThrow(() -> new ResourceNotFoundException("Project", id));
    }

    private ProjectResponse toResponse(Project p) {
        String owner = p.getOwnerId() == null ? null
                : employees.findById(p.getOwnerId()).map(e -> e.getFirstName() + " " + e.getLastName()).orElse(null);
        return new ProjectResponse(p.getId(), p.getName(), p.getDescription(), p.getStatus(), p.getOwnerId(), owner,
                p.getStartDate(), p.getDueDate(), tasks.countByProjectId(p.getId()),
                tasks.countByProjectIdAndStatus(p.getId(), TaskStatus.DONE), p.getBudget());
    }
}
