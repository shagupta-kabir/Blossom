package io.blossom.erp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskServiceTest {

    @Mock
    TaskRepository tasks;
    @Mock
    ProjectRepository projects;
    @Mock
    EmployeeRepository employees;

    TaskService service;
    Task task;

    final AuthUser assignee = new AuthUser("u1", "a@blossom.demo", "Assignee", Role.EMPLOYEE, "e1");
    final AuthUser stranger = new AuthUser("u2", "s@blossom.demo", "Stranger", Role.EMPLOYEE, "e2");
    final AuthUser manager = new AuthUser("u3", "m@blossom.demo", "Manager", Role.MANAGER, "m1");

    @BeforeEach
    void setUp() {
        service = new TaskService(tasks, projects, employees);
        task = new Task();
        task.setId("t1");
        task.setProjectId("p1");
        task.setTitle("Write tests");
        task.setStatus(TaskStatus.TODO);
        task.setAssigneeId("e1");
        when(tasks.findById("t1")).thenReturn(Optional.of(task));
        when(tasks.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void assigneeCanMoveOwnTask() {
        TaskResponse r = service.updateStatus("t1", new TaskStatusRequest(TaskStatus.IN_PROGRESS), assignee);
        assertThat(r.status()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void managerCanMoveAnyTask() {
        assertThat(service.updateStatus("t1", new TaskStatusRequest(TaskStatus.DONE), manager).status())
                .isEqualTo(TaskStatus.DONE);
    }

    @Test
    void otherEmployeeCannotMoveTask() {
        assertThatThrownBy(() -> service.updateStatus("t1", new TaskStatusRequest(TaskStatus.DONE), stranger))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void overdueFlagIsSetOnlyForOpenTasksPastDueDate() {
        task.setDueDate(LocalDate.now().minusDays(2));
        assertThat(service.get("t1").overdue()).isTrue();

        task.setStatus(TaskStatus.DONE);
        assertThat(service.get("t1").overdue()).isFalse();
    }

    @Test
    void createRequiresExistingProject() {
        when(projects.existsById("missing")).thenReturn(false);
        assertThatThrownBy(() -> service.create(new TaskRequest("missing", "Title", null, null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createAppliesDefaults() {
        when(projects.existsById("p1")).thenReturn(true);
        TaskResponse r = service.create(new TaskRequest("p1", "  Plan sprint  ", null, null, null, null, null));
        assertThat(r.title()).isEqualTo("Plan sprint");
        assertThat(r.status()).isEqualTo(TaskStatus.TODO);
        assertThat(r.priority().name()).isEqualTo("MEDIUM");
    }
}
