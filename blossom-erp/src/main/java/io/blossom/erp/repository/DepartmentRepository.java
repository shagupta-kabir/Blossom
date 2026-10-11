package io.blossom.erp.repository;

import io.blossom.erp.domain.Department;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DepartmentRepository extends MongoRepository<Department, String> {
    boolean existsByNameIgnoreCase(String name);
}
