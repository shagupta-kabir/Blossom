package io.blossom.erp.service;

import io.blossom.erp.domain.Employee;
import io.blossom.erp.domain.LeaveRequest;
import io.blossom.erp.domain.LeaveStatus;
import io.blossom.erp.domain.Role;
import io.blossom.erp.dto.LeaveDtos.LeaveCreateRequest;
import io.blossom.erp.dto.LeaveDtos.LeaveDecisionRequest;
import io.blossom.erp.dto.LeaveDtos.LeaveResponse;
import io.blossom.erp.exception.BadRequestException;
import io.blossom.erp.exception.ConflictException;
import io.blossom.erp.exception.ResourceNotFoundException;
import io.blossom.erp.repository.EmployeeRepository;
import io.blossom.erp.repository.LeaveRequestRepository;
import io.blossom.erp.security.AuthUser;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class LeaveService {

    private final LeaveRequestRepository leaves;
    private final EmployeeRepository employees;

    public LeaveService(LeaveRequestRepository leaves, EmployeeRepository employees) {
        this.leaves = leaves;
        this.employees = employees;
    }

    public LeaveResponse create(LeaveCreateRequest req, AuthUser user) {
        if (user.employeeId() == null) {
            throw new BadRequestException("Your account is not linked to an employee profile, so you cannot request leave");
        }
        Employee emp = employees.findById(user.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", user.employeeId()));
        if (req.endDate().isBefore(req.startDate())) {
            throw new BadRequestException("End date must not be before start date");
        }
        boolean overlaps = leaves.findByEmployeeIdAndStatusIn(emp.getId(), List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED))
                .stream()
                .anyMatch(l -> !req.startDate().isAfter(l.getEndDate()) && !req.endDate().isBefore(l.getStartDate()));
        if (overlaps) {
            throw new ConflictException("You already have a pending or approved leave request overlapping these dates");
        }
        LeaveRequest l = new LeaveRequest();
        l.setEmployeeId(emp.getId());
        l.setType(req.type());
        l.setStartDate(req.startDate());
        l.setEndDate(req.endDate());
        l.setDays(daysBetween(req.startDate(), req.endDate()));
        l.setReason(req.reason());
        l.setStatus(LeaveStatus.PENDING);
        l.setCreatedAt(Instant.now());
        return toResponses(List.of(leaves.save(l))).get(0);
    }

    public List<LeaveResponse> mine(AuthUser user) {
        if (user.employeeId() == null) {
            return List.of();
        }
        return toResponses(leaves.findByEmployeeIdOrderByCreatedAtDesc(user.employeeId()));
    }

    /** ADMIN and HR see everything; a MANAGER only sees requests from their direct reports. */
    public List<LeaveResponse> review(AuthUser user, LeaveStatus status) {
        List<LeaveRequest> rows;
        if (user.role() == Role.MANAGER) {
            if (user.employeeId() == null) {
                return List.of();
            }
            List<String> reportIds = employees.findByManagerId(user.employeeId()).stream().map(Employee::getId).toList();
            if (reportIds.isEmpty()) {
                return List.of();
            }
            rows = status == null
                    ? leaves.findByEmployeeIdInOrderByCreatedAtDesc(reportIds)
                    : leaves.findByEmployeeIdInAndStatusOrderByCreatedAtDesc(reportIds, status);
        } else {
            rows = status == null ? leaves.findAllByOrderByCreatedAtDesc() : leaves.findByStatusOrderByCreatedAtDesc(status);
        }
        return toResponses(rows);
    }

    public LeaveResponse decide(String id, LeaveDecisionRequest req, AuthUser user) {
        if (req.decision() != LeaveStatus.APPROVED && req.decision() != LeaveStatus.REJECTED) {
            throw new BadRequestException("Decision must be APPROVED or REJECTED");
        }
        LeaveRequest l = find(id);
        if (l.getStatus() != LeaveStatus.PENDING) {
            throw new ConflictException("Only pending requests can be decided (current status: " + l.getStatus() + ")");
        }
        if (l.getEmployeeId().equals(user.employeeId())) {
            throw new AccessDeniedException("You cannot decide your own leave request");
        }
        if (user.role() == Role.MANAGER) {
            Employee requester = employees.findById(l.getEmployeeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee", l.getEmployeeId()));
            if (user.employeeId() == null || !user.employeeId().equals(requester.getManagerId())) {
                throw new AccessDeniedException("Managers can only decide requests from their direct reports");
            }
        }
        l.setStatus(req.decision());
        l.setReviewerName(user.fullName());
        l.setReviewNote(req.note());
        l.setDecidedAt(Instant.now());
        return toResponses(List.of(leaves.save(l))).get(0);
    }

    public LeaveResponse cancel(String id, AuthUser user) {
        LeaveRequest l = find(id);
        if (user.employeeId() == null || !user.employeeId().equals(l.getEmployeeId())) {
            throw new AccessDeniedException("You can only cancel your own leave requests");
        }
        boolean cancellable = l.getStatus() == LeaveStatus.PENDING
                || (l.getStatus() == LeaveStatus.APPROVED && l.getStartDate().isAfter(LocalDate.now()));
        if (!cancellable) {
            throw new ConflictException("This request can no longer be cancelled");
        }
        l.setStatus(LeaveStatus.CANCELLED);
        l.setDecidedAt(Instant.now());
        return toResponses(List.of(leaves.save(l))).get(0);
    }

    static int daysBetween(LocalDate start, LocalDate end) {
        return (int) ChronoUnit.DAYS.between(start, end) + 1;
    }

    private LeaveRequest find(String id) {
        return leaves.findById(id).orElseThrow(() -> new ResourceNotFoundException("Leave request", id));
    }

    private List<LeaveResponse> toResponses(Collection<LeaveRequest> rows) {
        List<String> ids = rows.stream().map(LeaveRequest::getEmployeeId).distinct().toList();
        Map<String, String> names = employees.findAllById(ids).stream()
                .collect(Collectors.toMap(Employee::getId, e -> e.getFirstName() + " " + e.getLastName(), (a, b) -> a));
        return rows.stream().map(l -> new LeaveResponse(l.getId(), l.getEmployeeId(), names.get(l.getEmployeeId()),
                l.getType(), l.getStartDate(), l.getEndDate(), l.getDays(), l.getReason(), l.getStatus(),
                l.getReviewerName(), l.getReviewNote(), l.getCreatedAt(), l.getDecidedAt())).toList();
    }
}
