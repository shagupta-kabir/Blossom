package io.blossom.erp.repository;

import io.blossom.erp.domain.Employee;
import io.blossom.erp.domain.EmployeeStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EmployeeRepository extends MongoRepository<Employee, String> {
    Optional<Employee> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByDepartmentId(String departmentId);

    long countByStatus(EmployeeStatus status);

    boolean existsByManagerId(String managerId);

    List<Employee> findByManagerId(String managerId);
}
