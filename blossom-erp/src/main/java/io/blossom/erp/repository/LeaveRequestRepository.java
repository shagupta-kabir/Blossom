package io.blossom.erp.repository;

import io.blossom.erp.domain.LeaveRequest;
import io.blossom.erp.domain.LeaveStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LeaveRequestRepository extends MongoRepository<LeaveRequest, String> {
    List<LeaveRequest> findAllByOrderByCreatedAtDesc();

    List<LeaveRequest> findByStatusOrderByCreatedAtDesc(LeaveStatus status);

    List<LeaveRequest> findByEmployeeIdOrderByCreatedAtDesc(String employeeId);

    List<LeaveRequest> findByEmployeeIdInOrderByCreatedAtDesc(Collection<String> employeeIds);

    List<LeaveRequest> findByEmployeeIdInAndStatusOrderByCreatedAtDesc(Collection<String> employeeIds, LeaveStatus status);

    List<LeaveRequest> findByEmployeeIdAndStatusIn(String employeeId, Collection<LeaveStatus> statuses);

    long countByStatus(LeaveStatus status);

    long countByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(LeaveStatus status, LocalDate start, LocalDate end);
}
