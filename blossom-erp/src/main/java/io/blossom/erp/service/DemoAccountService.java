package io.blossom.erp.service;

import io.blossom.erp.domain.AppUser;
import io.blossom.erp.domain.Employee;
import io.blossom.erp.domain.Role;
import io.blossom.erp.dto.AuthDtos.AuthResponse;
import io.blossom.erp.dto.AuthDtos.UserResponse;
import io.blossom.erp.repository.AppUserRepository;
import io.blossom.erp.repository.EmployeeRepository;
import io.blossom.erp.security.JwtService;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Demo mode only (app.seed.demo-data=true): keeps the four demo logins alive and gives the login page
 * its one-click "instant login" buttons. When demo mode is off, nothing here does anything and
 * {@link #login(Role)} is refused.
 */
@Service
public class DemoAccountService {

    private static final Logger log = LoggerFactory.getLogger(DemoAccountService.class);

    private record Spec(Role role, String email, String name, String employeeEmail) {
    }

    private static final List<Spec> SPECS = List.of(
            new Spec(Role.ADMIN, "admin@blossom.demo", "Shagupta", "shagupta@blossom.demo"),
            new Spec(Role.HR, "hr@blossom.demo", "Abony", "Abony@blossom.demo"),
            new Spec(Role.MANAGER, "manager@blossom.demo", "Kabir", "Kabir@blossom.demo"),
            new Spec(Role.EMPLOYEE, "employee@blossom.demo", "Ani", "Ani@blossom.demo"));

    private final AppUserRepository users;
    private final EmployeeRepository employees;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final boolean enabled;
    private final String password;

    public DemoAccountService(AppUserRepository users, EmployeeRepository employees, PasswordEncoder encoder, JwtService jwt,
                              @Value("${app.seed.demo-data:true}") boolean enabled,
                              @Value("${app.seed.demo-password:demo1234}") String password) {
        this.users = users;
        this.employees = employees;
        this.encoder = encoder;
        this.jwt = jwt;
        this.enabled = enabled;
        this.password = password;
    }

    /** Creates or repairs all four demo accounts (active, right role, current demo password). */
    public void ensureAll() {
        SPECS.forEach(this::ensure);
    }

    /** Signs in as the demo account for the given role without typing a password. Refused unless demo mode is on. */
    public AuthResponse login(Role role) {
        if (!enabled) {
            throw new AccessDeniedException("Demo mode is off. Set SEED_DEMO_DATA=true to use instant demo logins.");
        }
        Spec spec = SPECS.stream().filter(s -> s.role() == role).findFirst()
                .orElseThrow(() -> new AccessDeniedException("There is no demo account for that role"));
        AppUser user = ensure(spec);
        return new AuthResponse(jwt.generate(user), "Bearer", jwt.expiresInSeconds(), UserResponse.from(user));
    }

    private AppUser ensure(Spec spec) {
        String employeeId = employees.findByEmail(spec.employeeEmail()).map(Employee::getId).orElse(null);
        AppUser u = users.findByEmail(spec.email()).orElse(null);
        if (u == null) {
            u = new AppUser();
            u.setEmail(spec.email());
            u.setFullName(spec.name());
            u.setRole(spec.role());
            u.setEmployeeId(employeeId);
            u.setPasswordHash(encoder.encode(password));
            u.setActive(true);
            u.setCreatedAt(Instant.now());
            log.info("Created missing demo account {}", spec.email());
            return users.save(u);
        }
        boolean changed = false;
        if (!u.isActive()) { u.setActive(true); changed = true; }
        if (u.getRole() != spec.role()) { u.setRole(spec.role()); changed = true; }
        if (u.getEmployeeId() == null && employeeId != null) { u.setEmployeeId(employeeId); changed = true; }
        if (u.getPasswordHash() == null || !encoder.matches(password, u.getPasswordHash())) {
            u.setPasswordHash(encoder.encode(password));
            changed = true;
        }
        if (changed) {
            log.info("Repaired demo account {}", spec.email());
            return users.save(u);
        }
        return u;
    }
}
