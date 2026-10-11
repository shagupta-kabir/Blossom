package io.blossom.erp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.blossom.erp.domain.Department;
import io.blossom.erp.domain.Employee;
import io.blossom.erp.domain.Project;
import io.blossom.erp.domain.EmployeeStatus;
import io.blossom.erp.domain.LeaveStatus;
import io.blossom.erp.domain.ProjectStatus;
import io.blossom.erp.domain.TaskStatus;
import io.blossom.erp.dto.DashboardResponse;
import io.blossom.erp.repository.AnnouncementRepository;
import io.blossom.erp.repository.DepartmentRepository;
import io.blossom.erp.repository.EmployeeRepository;
import io.blossom.erp.repository.LeaveRequestRepository;
import io.blossom.erp.repository.ProjectRepository;
import io.blossom.erp.repository.TaskRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardServiceTest {

    @Mock
    EmployeeRepository employees;
    @Mock
    DepartmentRepository departments;
    @Mock
    LeaveRequestRepository leaves;
    @Mock
    ProjectRepository projects;
    @Mock
    TaskRepository tasks;
    @Mock
    AnnouncementRepository announcements;

    private static Employee salaried(EmployeeStatus status, long salary) {
        Employee e = new Employee();
        e.setStatus(status);
        e.setMonthlySalary(salary);
        return e;
    }

    private static Project budgeted(ProjectStatus status, long budget) {
        Project p = new Project();
        p.setStatus(status);
        p.setBudget(budget);
        return p;
    }

    @Test
    void payrollIsHiddenFromNonPrivilegedUsers() {
        Employee a = salaried(EmployeeStatus.ACTIVE, 100_000L);
        when(employees.findAll()).thenReturn(List.of(a));

        DashboardResponse r = new DashboardService(employees, departments, leaves, projects, tasks, announcements).stats(false);
        assertThat(r.monthlyPayroll()).isNull();
    }

    @Test
    void aggregatesCountsFromRepositories() {
        Department small = new Department();
        small.setId("d1");
        small.setName("Finance");
        Department big = new Department();
        big.setId("d2");
        big.setName("Engineering");

        when(employees.count()).thenReturn(10L);
        when(employees.countByStatus(EmployeeStatus.ACTIVE)).thenReturn(9L);
        when(departments.count()).thenReturn(2L);
        when(departments.findAll()).thenReturn(List.of(small, big));
        when(employees.countByDepartmentId("d1")).thenReturn(2L);
        when(employees.countByDepartmentId("d2")).thenReturn(7L);
        when(leaves.countByStatus(LeaveStatus.PENDING)).thenReturn(3L);
        when(leaves.countByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                any(LeaveStatus.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(1L);
        when(projects.countByStatusNot(ProjectStatus.COMPLETED)).thenReturn(4L);
        when(projects.countByStatus(ProjectStatus.ACTIVE)).thenReturn(2L);
        when(tasks.countByStatusNot(TaskStatus.DONE)).thenReturn(12L);
        when(tasks.countByStatus(TaskStatus.TODO)).thenReturn(5L);
        when(tasks.countByStatusNotAndDueDateBefore(any(TaskStatus.class), any(LocalDate.class))).thenReturn(2L);
        Employee a = salaried(EmployeeStatus.ACTIVE, 100_000L);
        Employee b = salaried(EmployeeStatus.ACTIVE, 65_500L);
        Employee gone = salaried(EmployeeStatus.TERMINATED, 999_999L);
        when(employees.findAll()).thenReturn(List.of(a, b, gone));
        Project open = budgeted(ProjectStatus.ACTIVE, 2_500_000L);
        Project done = budgeted(ProjectStatus.COMPLETED, 600_000L);
        when(projects.findAll()).thenReturn(List.of(open, done));
        when(announcements.findTop5ByOrderByPinnedDescCreatedAtDesc()).thenReturn(List.of());

        DashboardResponse r = new DashboardService(employees, departments, leaves, projects, tasks, announcements).stats(true);

        assertThat(r.totalEmployees()).isEqualTo(10);
        assertThat(r.activeEmployees()).isEqualTo(9);
        assertThat(r.pendingLeaveRequests()).isEqualTo(3);
        assertThat(r.onLeaveToday()).isEqualTo(1);
        assertThat(r.openProjects()).isEqualTo(4);
        assertThat(r.openTasks()).isEqualTo(12);
        assertThat(r.overdueTasks()).isEqualTo(2);
        assertThat(r.tasksByStatus()).containsEntry("TODO", 5L).containsKeys("IN_PROGRESS", "REVIEW", "DONE");
        assertThat(r.currency()).isEqualTo("BDT");
        assertThat(r.monthlyPayroll()).isEqualTo(165_500L);
        assertThat(r.openProjectBudget()).isEqualTo(2_500_000L);
        assertThat(r.projectsByStatus()).containsEntry("ACTIVE", 2L);
        assertThat(r.headcountByDepartment()).extracting(DashboardResponse.DepartmentHeadcount::departmentName).containsExactly("Engineering", "Finance");
    }
}
