package io.blossom.erp.dto;

import io.blossom.erp.domain.EmployeeStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public final class OrgDtos {
    private OrgDtos() {
    }

    public record DepartmentRequest(@NotBlank @Size(max = 80) String name,
                                    @Size(max = 500) String description,
                                    String headEmployeeId) {
    }

    public record DepartmentResponse(String id, String name, String description, String headEmployeeId,
                                     String headEmployeeName, long employeeCount) {
    }

    public record EmployeeRequest(@NotBlank @Size(max = 60) String firstName,
                                  @NotBlank @Size(max = 60) String lastName,
                                  @NotBlank @Email String email,
                                  @Pattern(regexp = "^(\\+8801|01)[3-9]\\d{8}$", message = "must be a valid Bangladeshi mobile number, e.g. +8801712345678") String phone,
                                  @NotBlank @Size(max = 100) String jobTitle,
                                  String departmentId,
                                  String managerId,
                                  @NotNull LocalDate hireDate,
                                  EmployeeStatus status,
                                  @Size(max = 60) String city,
                                  @PositiveOrZero Long monthlySalary) {
    }

    public record EmployeeResponse(String id, String firstName, String lastName, String fullName, String email,
                                   String phone, String jobTitle, String departmentId, String departmentName,
                                   String managerId, LocalDate hireDate, EmployeeStatus status,
                                   String city, Long monthlySalary) {
    }
}
