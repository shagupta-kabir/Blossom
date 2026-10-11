package io.blossom.erp.repository;

import io.blossom.erp.domain.Project;
import io.blossom.erp.domain.ProjectStatus;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProjectRepository extends MongoRepository<Project, String> {
    List<Project> findAllByOrderByCreatedAtDesc();

    long countByStatus(ProjectStatus status);

    long countByStatusNot(ProjectStatus status);
}
