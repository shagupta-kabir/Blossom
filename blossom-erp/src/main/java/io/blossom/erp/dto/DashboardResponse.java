package io.blossom.erp.dto;

import java.util.List;
import java.util.Map;

/**
 * Money amounts are whole Bangladeshi Taka (currency code "BDT", shown as "Tk").
 * monthlyPayroll is only filled in for ADMIN and HR users, it is null for everybody else.
 */
public record DashboardResponse(long totalEmployees,
                                long activeEmployees,
                                long departments,
                                long pendingLeaveRequests,
                                long onLeaveToday,
                                long openProjects,
                                long openTasks,
                                long overdueTasks,
                                String currency,
                                Long monthlyPayroll,
                                long openProjectBudget,
                                Map<String, Long> tasksByStatus,
                                Map<String, Long> projectsByStatus,
                                List<DepartmentHeadcount> headcountByDepartment,
                                List<WorkDtos.AnnouncementResponse> latestAnnouncements) {

    public record DepartmentHeadcount(String departmentId, String departmentName, long employees) {
    }
}
