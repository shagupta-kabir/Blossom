package io.blossom.erp.service;

import io.blossom.erp.domain.AppUser;
import io.blossom.erp.dto.AuthDtos.CreateUserRequest;
import io.blossom.erp.dto.AuthDtos.UpdateUserRequest;
import io.blossom.erp.dto.AuthDtos.UserResponse;
import io.blossom.erp.exception.ConflictException;
import io.blossom.erp.exception.ResourceNotFoundException;
import io.blossom.erp.repository.AppUserRepository;
import io.blossom.erp.repository.EmployeeRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserService {

    private final AppUserRepository users;
    private final EmployeeRepository employees;
    private final PasswordEncoder encoder;

    public UserService(AppUserRepository users, EmployeeRepository employees, PasswordEncoder encoder) {
        this.users = users;
        this.employees = employees;
        this.encoder = encoder;
    }

    public List<UserResponse> list() {
        return users.findAll().stream().map(UserResponse::from).toList();
    }

    public UserResponse create(CreateUserRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ConflictException("A user with this email already exists");
        }
        requireEmployee(req.employeeId());
        AppUser u = new AppUser();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(req.password()));
        u.setFullName(req.fullName().trim());
        u.setRole(req.role());
        u.setEmployeeId(StringUtils.hasText(req.employeeId()) ? req.employeeId() : null);
        u.setActive(true);
        u.setCreatedAt(Instant.now());
        return UserResponse.from(users.save(u));
    }

    public UserResponse update(String id, UpdateUserRequest req) {
        AppUser u = users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (req.role() != null) {
            u.setRole(req.role());
        }
        if (req.active() != null) {
            u.setActive(req.active());
        }
        if (req.employeeId() != null) {
            requireEmployee(req.employeeId());
            u.setEmployeeId(req.employeeId().isBlank() ? null : req.employeeId());
        }
        return UserResponse.from(users.save(u));
    }

    private void requireEmployee(String employeeId) {
        if (StringUtils.hasText(employeeId) && !employees.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee", employeeId);
        }
    }
}
