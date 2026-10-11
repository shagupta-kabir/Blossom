package io.blossom.erp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.blossom.erp.domain.Employee;
import io.blossom.erp.domain.LeaveRequest;
import io.blossom.erp.domain.LeaveStatus;
import io.blossom.erp.domain.LeaveType;
import io.blossom.erp.domain.Role;
import io.blossom.erp.dto.LeaveDtos.LeaveCreateRequest;
import io.blossom.erp.dto.LeaveDtos.LeaveDecisionRequest;
import io.blossom.erp.dto.LeaveDtos.LeaveResponse;
import io.blossom.erp.exception.BadRequestException;
import io.blossom.erp.exception.ConflictException;
import io.blossom.erp.repository.EmployeeRepository;
import io.blossom.erp.repository.LeaveRequestRepository;
import io.blossom.erp.security.AuthUser;
import java.time.LocalDate;
import java.util.List;
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
class LeaveServiceTest {

    @Mock
    LeaveRequestRepository leaves;
    @Mock
    EmployeeRepository employees;

    LeaveService service;

    final AuthUser worker = new AuthUser("u1", "w@blossom.demo", "Worker One", Role.EMPLOYEE, "e1");
    final AuthUser theirManager = new AuthUser("u2", "m@blossom.demo", "Manager One", Role.MANAGER, "m1");
    final AuthUser otherManager = new AuthUser("u3", "o@blossom.demo", "Other Manager", Role.MANAGER, "m2");
    final AuthUser hr = new AuthUser("u4", "hr@blossom.demo", "HR One", Role.HR, "h1");

    @BeforeEach
    void setUp() {
        service = new LeaveService(leaves, employees);
        when(leaves.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(employees.findById("e1")).thenReturn(Optional.of(employee("e1", "m1")));
    }

    private static Employee employee(String id, String managerId) {
        Employee e = new Employee();
        e.setId(id);
        e.setFirstName("First" + id);
        e.setLastName("Last");
        e.setManagerId(managerId);
        return e;
    }

    private LeaveRequest pendingLeaveOf(String employeeId) {
        LeaveRequest l = new LeaveRequest();
        l.setId("l1");
        l.setEmployeeId(employeeId);
        l.setType(LeaveType.ANNUAL);
        l.setStartDate(LocalDate.of(2026, 3, 2));
        l.setEndDate(LocalDate.of(2026, 3, 4));
        l.setStatus(LeaveStatus.PENDING);
        when(leaves.findById("l1")).thenReturn(Optional.of(l));
        return l;
    }

    @Test
    void createCountsInclusiveDaysAndStartsPending() {
        LeaveResponse r = service.create(new LeaveCreateRequest(LeaveType.ANNUAL,
                LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 6), "Trip"), worker);

        assertThat(r.days()).isEqualTo(5);
        assertThat(r.status()).isEqualTo(LeaveStatus.PENDING);
        assertThat(r.employeeId()).isEqualTo("e1");
    }

    @Test
    void createRejectsEndBeforeStart() {
        assertThatThrownBy(() -> service.create(new LeaveCreateRequest(LeaveType.SICK,
                LocalDate.of(2026, 3, 6), LocalDate.of(2026, 3, 2), null), worker))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createRejectsOverlapWithExistingLeave() {
        LeaveRequest existing = new LeaveRequest();
        existing.setStartDate(LocalDate.of(2026, 3, 4));
        existing.setEndDate(LocalDate.of(2026, 3, 10));
        when(leaves.findByEmployeeIdAndStatusIn(eq("e1"), anyCollection())).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.create(new LeaveCreateRequest(LeaveType.ANNUAL,
                LocalDate.of(2026, 3, 8), LocalDate.of(2026, 3, 12), null), worker))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createAllowsBackToBackLeave() {
        LeaveRequest existing = new LeaveRequest();
        existing.setStartDate(LocalDate.of(2026, 3, 1));
        existing.setEndDate(LocalDate.of(2026, 3, 5));
        when(leaves.findByEmployeeIdAndStatusIn(eq("e1"), anyCollection())).thenReturn(List.of(existing));

        LeaveResponse r = service.create(new LeaveCreateRequest(LeaveType.ANNUAL,
                LocalDate.of(2026, 3, 6), LocalDate.of(2026, 3, 7), null), worker);
        assertThat(r.days()).isEqualTo(2);
    }

    @Test
    void accountWithoutEmployeeProfileCannotRequestLeave() {
        AuthUser noProfile = new AuthUser("u9", "x@blossom.demo", "No Profile", Role.ADMIN, null);
        assertThatThrownBy(() -> service.create(new LeaveCreateRequest(LeaveType.ANNUAL,
                LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 3), null), noProfile))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void managerCanApproveDirectReport() {
        pendingLeaveOf("e1");
        LeaveResponse r = service.decide("l1", new LeaveDecisionRequest(LeaveStatus.APPROVED, "Enjoy"), theirManager);

        assertThat(r.status()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(r.reviewerName()).isEqualTo("Manager One");
        assertThat(r.reviewNote()).isEqualTo("Enjoy");
        assertThat(r.decidedAt()).isNotNull();
    }

    @Test
    void managerCannotDecideForSomeoneElsesReport() {
        pendingLeaveOf("e1");
        assertThatThrownBy(() -> service.decide("l1", new LeaveDecisionRequest(LeaveStatus.APPROVED, null), otherManager))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void hrCanDecideAnyRequest() {
        pendingLeaveOf("e1");
        LeaveResponse r = service.decide("l1", new LeaveDecisionRequest(LeaveStatus.REJECTED, "Busy period"), hr);
        assertThat(r.status()).isEqualTo(LeaveStatus.REJECTED);
    }

    @Test
    void nobodyCanDecideTheirOwnRequest() {
        pendingLeaveOf("h1");
        assertThatThrownBy(() -> service.decide("l1", new LeaveDecisionRequest(LeaveStatus.APPROVED, null), hr))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void decisionMustBeApprovedOrRejected() {
        pendingLeaveOf("e1");
        assertThatThrownBy(() -> service.decide("l1", new LeaveDecisionRequest(LeaveStatus.CANCELLED, null), hr))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void alreadyDecidedRequestCannotBeDecidedAgain() {
        LeaveRequest l = pendingLeaveOf("e1");
        l.setStatus(LeaveStatus.APPROVED);
        assertThatThrownBy(() -> service.decide("l1", new LeaveDecisionRequest(LeaveStatus.REJECTED, null), hr))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void ownerCanCancelPendingRequest() {
        pendingLeaveOf("e1");
        assertThat(service.cancel("l1", worker).status()).isEqualTo(LeaveStatus.CANCELLED);
    }

    @Test
    void onlyOwnerCanCancel() {
        pendingLeaveOf("e1");
        assertThatThrownBy(() -> service.cancel("l1", hr)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void managerReviewListIsEmptyWithoutDirectReports() {
        when(employees.findByManagerId("m2")).thenReturn(List.of());
        assertThat(service.review(otherManager, null)).isEmpty();
    }
}
