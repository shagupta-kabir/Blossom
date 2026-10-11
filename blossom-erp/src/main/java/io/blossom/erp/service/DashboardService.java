package io.blossom.erp.service;

import io.blossom.erp.domain.Employee;
import io.blossom.erp.domain.EmployeeStatus;
import io.blossom.erp.domain.LeaveStatus;
import io.blossom.erp.domain.Project;
import io.blossom.erp.domain.ProjectStatus;
import io.blossom.erp.domain.TaskStatus;
import io.blossom.erp.dto.DashboardResponse;
import io.blossom.erp.dto.DashboardResponse.DepartmentHeadcount;
import io.blossom.erp.repository.AnnouncementRepository;
import io.blossom.erp.repository.DepartmentRepository;
import io.blossom.erp.repository.EmployeeRepository;
import io.blossom.erp.repository.LeaveRequestRepository;
import io.blossom.erp.repository.ProjectRepository;
import io.blossom.erp.repository.TaskRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final EmployeeRepository employees;
    private final DepartmentRepository departments;
    private final LeaveRequestRepository leaves;
    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final AnnouncementRepository announcements;

    public DashboardService(EmployeeRepository employees, DepartmentRepository departments, LeaveRequestRepository leaves,
                            ProjectRepository projects, TaskRepository tasks, AnnouncementRepository announcements) {
        this.employees = employees;
        this.departments = departments;
        this.leaves = leaves;
        this.projects = projects;
        this.tasks = tasks;
        this.announcements = announcements;
    }

    /** @param includePayroll true for ADMIN/HR; payroll figures are confidential. */
    public DashboardResponse stats(boolean includePayroll) {
        LocalDate today = LocalDate.now();
        Map<String, Long> taskCounts = new LinkedHashMap<>();
        for (TaskStatus s : TaskStatus.values()) {
            taskCounts.put(s.name(), tasks.countByStatus(s));
        }
        Map<String, Long> projectCounts = new LinkedHashMap<>();
        for (ProjectStatus s : ProjectStatus.values()) {
            projectCounts.put(s.name(), projects.countByStatus(s));
        }
        List<DepartmentHeadcount> headcount = departments.findAll().stream()
                .map(d -> new DepartmentHeadcount(d.getId(), d.getName(), employees.countByDepartmentId(d.getId())))
                .sorted(Comparator.comparingLong(DepartmentHeadcount::employees).reversed())
                .toList();
        Long payroll = includePayroll
                ? employees.findAll().stream()
                        .filter(e -> e.getStatus() == EmployeeStatus.ACTIVE && e.getMonthlySalary() != null)
                        .mapToLong(Employee::getMonthlySalary).sum()
                : null;
        long openBudget = projects.findAll().stream()
                .filter(p -> p.getStatus() != ProjectStatus.COMPLETED && p.getBudget() != null)
                .mapToLong(Project::getBudget).sum();
        return new DashboardResponse(
                employees.count(),
                employees.countByStatus(EmployeeStatus.ACTIVE),
                departments.count(),
                leaves.countByStatus(LeaveStatus.PENDING),
                leaves.countByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(LeaveStatus.APPROVED, today, today),
                projects.countByStatusNot(ProjectStatus.COMPLETED),
                tasks.countByStatusNot(TaskStatus.DONE),
                tasks.countByStatusNotAndDueDateBefore(TaskStatus.DONE, today),
                "BDT", payroll, openBudget,
                taskCounts, projectCounts, headcount,
                announcements.findTop5ByOrderByPinnedDescCreatedAtDesc().stream().map(AnnouncementService::toResponse).toList());
    }
}
