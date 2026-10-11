package io.blossom.erp.service;

import io.blossom.erp.domain.AppUser;
import io.blossom.erp.domain.Employee;
import io.blossom.erp.domain.EmployeeStatus;
import io.blossom.erp.domain.Role;
import io.blossom.erp.dto.AuthDtos.AuthResponse;
import io.blossom.erp.dto.AuthDtos.ChangePasswordRequest;
import io.blossom.erp.dto.AuthDtos.LoginRequest;
import io.blossom.erp.dto.AuthDtos.RegisterRequest;
import io.blossom.erp.dto.AuthDtos.RegisterResponse;
import io.blossom.erp.dto.AuthDtos.UserResponse;
import io.blossom.erp.exception.BadRequestException;
import io.blossom.erp.exception.ConflictException;
import io.blossom.erp.exception.ResourceNotFoundException;
import io.blossom.erp.repository.AppUserRepository;
import io.blossom.erp.repository.EmployeeRepository;
import io.blossom.erp.security.JwtService;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private final AppUserRepository users;
    private final EmployeeRepository employees;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final boolean signupEnabled;
    private final boolean autoApprove;
    private final String signupCode;

    public AuthService(AppUserRepository users, EmployeeRepository employees, PasswordEncoder encoder, JwtService jwt,
                       @Value("${app.signup.enabled:true}") boolean signupEnabled,
                       @Value("${app.signup.auto-approve:true}") boolean autoApprove,
                       @Value("${app.signup.code:}") String signupCode) {
        this.users = users;
        this.employees = employees;
        this.encoder = encoder;
        this.jwt = jwt;
        this.signupEnabled = signupEnabled;
        this.autoApprove = autoApprove;
        this.signupCode = signupCode;
    }

    public AuthResponse login(LoginRequest req) {
        AppUser user = users.findByEmail(req.email().trim().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        if (!user.isActive()) {
            throw new AccessDeniedException("Your account is waiting for approval or has been disabled. Please contact an administrator.");
        }
        return toAuthResponse(user);
    }

    /** Self sign-up: creates an employee profile plus an EMPLOYEE login. */
    public RegisterResponse register(RegisterRequest req) {
        if (!signupEnabled) {
            throw new AccessDeniedException("Sign-up is disabled. Ask an administrator to create your account.");
        }
        if (StringUtils.hasText(signupCode) && !signupCode.equals(req.signupCode())) {
            throw new AccessDeniedException("Invalid sign-up code");
        }
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email) || employees.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        Employee emp = new Employee();
        emp.setFirstName(req.firstName().trim());
        emp.setLastName(req.lastName().trim());
        emp.setEmail(email);
        emp.setJobTitle("New joiner");
        emp.setHireDate(LocalDate.now());
        emp.setStatus(EmployeeStatus.ACTIVE);
        emp.setCreatedAt(Instant.now());
        emp = employees.save(emp);

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(req.password()));
        user.setFullName(emp.getFirstName() + " " + emp.getLastName());
        user.setRole(Role.EMPLOYEE);
        user.setEmployeeId(emp.getId());
        user.setActive(autoApprove);
        user.setCreatedAt(Instant.now());
        user = users.save(user);

        if (autoApprove) {
            return new RegisterResponse("Account created. Welcome to Blossom ERP!", true, toAuthResponse(user));
        }
        return new RegisterResponse("Account created. An administrator must approve it before you can sign in.", false, null);
    }

    public UserResponse me(String userId) {
        return UserResponse.from(users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId)));
    }

    public void changePassword(String userId, ChangePasswordRequest req) {
        AppUser user = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (!encoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPasswordHash(encoder.encode(req.newPassword()));
        users.save(user);
    }

    private AuthResponse toAuthResponse(AppUser user) {
        return new AuthResponse(jwt.generate(user), "Bearer", jwt.expiresInSeconds(), UserResponse.from(user));
    }
}
