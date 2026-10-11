package io.blossom.erp.dto;

import io.blossom.erp.domain.Priority;
import io.blossom.erp.domain.ProjectStatus;
import io.blossom.erp.domain.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

public final class WorkDtos {
    private WorkDtos() {
    }

    public record ProjectRequest(@NotBlank @Size(max = 120) String name,
                                 @Size(max = 2000) String description,
                                 ProjectStatus status,
                                 String ownerId,
                                 LocalDate startDate,
                                 LocalDate dueDate,
                                 @PositiveOrZero Long budget) {
    }

    public record ProjectResponse(String id, String name, String description, ProjectStatus status, String ownerId,
                                  String ownerName, LocalDate startDate, LocalDate dueDate,
                                  long taskCount, long doneCount, Long budget) {
    }

    public record TaskRequest(@NotBlank String projectId,
                              @NotBlank @Size(max = 150) String title,
                              @Size(max = 2000) String description,
                              TaskStatus status,
                              Priority priority,
                              String assigneeId,
                              LocalDate dueDate) {
    }

    public record TaskStatusRequest(@NotNull TaskStatus status) {
    }

    public record TaskResponse(String id, String projectId, String projectName, String title, String description,
                               TaskStatus status, Priority priority, String assigneeId, String assigneeName,
                               LocalDate dueDate, boolean overdue) {
    }

    public record AnnouncementRequest(@NotBlank @Size(max = 150) String title,
                                      @NotBlank @Size(max = 5000) String body,
                                      boolean pinned) {
    }

    public record AnnouncementResponse(String id, String title, String body, boolean pinned,
                                       String authorName, Instant createdAt) {
    }
}
