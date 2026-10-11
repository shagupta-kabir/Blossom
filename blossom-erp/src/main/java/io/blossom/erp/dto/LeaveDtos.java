package io.blossom.erp.dto;

import io.blossom.erp.domain.LeaveStatus;
import io.blossom.erp.domain.LeaveType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

public final class LeaveDtos {
    private LeaveDtos() {
    }

    public record LeaveCreateRequest(@NotNull LeaveType type,
                                     @NotNull LocalDate startDate,
                                     @NotNull LocalDate endDate,
                                     @Size(max = 500) String reason) {
    }

    /** decision must be APPROVED or REJECTED. */
    public record LeaveDecisionRequest(@NotNull LeaveStatus decision, @Size(max = 500) String note) {
    }

    public record LeaveResponse(String id, String employeeId, String employeeName, LeaveType type,
                                LocalDate startDate, LocalDate endDate, int days, String reason,
                                LeaveStatus status, String reviewerName, String reviewNote,
                                Instant createdAt, Instant decidedAt) {
    }
}
