package io.blossom.erp.repository;

import io.blossom.erp.domain.Task;
import io.blossom.erp.domain.TaskStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TaskRepository extends MongoRepository<Task, String> {
    List<Task> findAllByOrderByCreatedAtDesc();

    List<Task> findByProjectIdOrderByCreatedAtDesc(String projectId);

    List<Task> findByAssigneeIdOrderByCreatedAtDesc(String assigneeId);

    long countByProjectId(String projectId);

    long countByProjectIdAndStatus(String projectId, TaskStatus status);

    long countByStatus(TaskStatus status);

    long countByStatusNot(TaskStatus status);

    long countByStatusNotAndDueDateBefore(TaskStatus status, LocalDate date);

    void deleteByProjectId(String projectId);
}
