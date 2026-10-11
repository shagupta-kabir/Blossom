package io.blossom.erp.controller;

import io.blossom.erp.domain.TaskStatus;
import io.blossom.erp.dto.WorkDtos.TaskRequest;
import io.blossom.erp.dto.WorkDtos.TaskResponse;
import io.blossom.erp.dto.WorkDtos.TaskStatusRequest;
import io.blossom.erp.security.AuthUser;
import io.blossom.erp.service.TaskService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService tasks;

    public TaskController(TaskService tasks) {
        this.tasks = tasks;
    }

    @GetMapping
    public List<TaskResponse> list(@RequestParam(required = false) String projectId,
                                   @RequestParam(required = false) String assigneeId,
                                   @RequestParam(required = false) TaskStatus status,
                                   @RequestParam(defaultValue = "false") boolean mine,
                                   @AuthenticationPrincipal AuthUser user) {
        return tasks.list(projectId, assigneeId, status, mine, user);
    }

    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable String id) {
        return tasks.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@Valid @RequestBody TaskRequest req) {
        return tasks.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public TaskResponse update(@PathVariable String id, @Valid @RequestBody TaskRequest req) {
        return tasks.update(id, req);
    }

    @PatchMapping("/{id}/status")
    public TaskResponse updateStatus(@PathVariable String id, @Valid @RequestBody TaskStatusRequest req,
                                     @AuthenticationPrincipal AuthUser user) {
        return tasks.updateStatus(id, req, user);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        tasks.delete(id);
    }
}
