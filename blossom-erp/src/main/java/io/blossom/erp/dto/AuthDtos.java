package io.blossom.erp.dto;

import io.blossom.erp.domain.AppUser;
import io.blossom.erp.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record ChangePasswordRequest(@NotBlank String currentPassword,
                                        @NotBlank @Size(min = 8, max = 72) String newPassword) {
    }

    public record CreateUserRequest(@NotBlank @Email String email,
                                    @NotBlank @Size(min = 8, max = 72) String password,
                                    @NotBlank @Size(max = 120) String fullName,
                                    @NotNull Role role,
                                    String employeeId) {
    }

    public record UpdateUserRequest(Role role, Boolean active, String employeeId) {
    }

    public record UserResponse(String id, String email, String fullName, Role role, String employeeId, boolean active) {
        public static UserResponse from(AppUser u) {
            return new UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.getEmployeeId(), u.isActive());
        }
    }

    public record AuthResponse(String token, String tokenType, long expiresInSeconds, UserResponse user) {
    }

    /** Public self-registration. The new account is always an EMPLOYEE; roles can only be raised by an admin. */
    public record RegisterRequest(@NotBlank @Size(max = 60) String firstName,
                                  @NotBlank @Size(max = 60) String lastName,
                                  @NotBlank @Email String email,
                                  @NotBlank @Size(min = 8, max = 72) String password,
                                  @Size(max = 100) String signupCode) {
    }

    /** auth is null when the account still has to be approved by an administrator. */
    public record RegisterResponse(String message, boolean active, AuthResponse auth) {
    }
}
