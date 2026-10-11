package io.blossom.erp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.blossom.erp.domain.AppUser;
import io.blossom.erp.domain.Employee;
import io.blossom.erp.domain.Role;
import io.blossom.erp.dto.AuthDtos.AuthResponse;
import io.blossom.erp.dto.AuthDtos.LoginRequest;
import io.blossom.erp.dto.AuthDtos.RegisterRequest;
import io.blossom.erp.dto.AuthDtos.RegisterResponse;
import io.blossom.erp.exception.ConflictException;
import io.blossom.erp.repository.AppUserRepository;
import io.blossom.erp.repository.EmployeeRepository;
import io.blossom.erp.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    AppUserRepository users;
    @Mock
    EmployeeRepository employees;
    @Mock
    PasswordEncoder encoder;

    final JwtService jwt = new JwtService("auth-test-secret-auth-test-secret-123456", 60);

    @BeforeEach
    void setUp() {
        when(encoder.encode(any())).thenAnswer(inv -> "hash:" + inv.getArgument(0));
        when(encoder.matches(any(), any())).thenAnswer(inv -> ("hash:" + inv.getArgument(0)).equals(inv.getArgument(1)));
        when(employees.save(any(Employee.class))).thenAnswer(inv -> {
            Employee e = inv.getArgument(0);
            e.setId("emp-1");
            return e;
        });
        when(users.save(any(AppUser.class))).thenAnswer(inv -> {
            AppUser u = inv.getArgument(0);
            u.setId("user-1");
            return u;
        });
    }

    private AuthService service(boolean enabled, boolean autoApprove, String code) {
        return new AuthService(users, employees, encoder, jwt, enabled, autoApprove, code);
    }

    private static RegisterRequest request(String email, String code) {
        return new RegisterRequest("Abony", "Rahman", email, "Secret#123", code);
    }

    @Test
    void signUpCreatesEmployeeRoleAccountAndLogsInWhenAutoApproved() {
        RegisterResponse r = service(true, true, "").register(request("  Abony@Example.com ", null));

        assertThat(r.active()).isTrue();
        AuthResponse auth = r.auth();
        assertThat(auth).isNotNull();
        assertThat(auth.token()).isNotBlank();
        assertThat(auth.user().role()).isEqualTo(Role.EMPLOYEE);
        assertThat(auth.user().email()).isEqualTo("abony@example.com");
        assertThat(auth.user().fullName()).isEqualTo("Abony Rahman");
        assertThat(auth.user().employeeId()).isEqualTo("emp-1");
    }

    @Test
    void signUpNeverGrantsAPrivilegedRole() {
        service(true, true, "").register(request("a@example.com", null));

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getRole()).isEqualTo(Role.EMPLOYEE);
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hash:Secret#123");
    }

    @Test
    void signUpWaitsForApprovalWhenAutoApproveIsOff() {
        RegisterResponse r = service(true, false, "").register(request("a@example.com", null));

        assertThat(r.active()).isFalse();
        assertThat(r.auth()).isNull();
        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().isActive()).isFalse();
    }

    @Test
    void signUpRejectsDuplicateEmailInUsersOrEmployees() {
        when(users.existsByEmail("dup@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service(true, true, "").register(request("dup@example.com", null)))
                .isInstanceOf(ConflictException.class);

        when(employees.existsByEmail("staff@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service(true, true, "").register(request("staff@example.com", null)))
                .isInstanceOf(ConflictException.class);
        verify(users, never()).save(any(AppUser.class));
    }

    @Test
    void signUpCanBeDisabled() {
        assertThatThrownBy(() -> service(false, true, "").register(request("a@example.com", null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void signUpCodeIsEnforcedWhenConfigured() {
        AuthService s = service(true, true, "FACULTY2026");
        assertThatThrownBy(() -> s.register(request("a@example.com", "wrong"))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> s.register(request("a@example.com", null))).isInstanceOf(AccessDeniedException.class);
        assertThat(s.register(request("a@example.com", "FACULTY2026")).active()).isTrue();
    }

    private AppUser storedUser(boolean active) {
        AppUser u = new AppUser();
        u.setId("u9");
        u.setEmail("hr@blossom.demo");
        u.setFullName("Farzana Akter");
        u.setRole(Role.HR);
        u.setPasswordHash("hash:Demo@12345");
        u.setActive(active);
        when(users.findByEmail("hr@blossom.demo")).thenReturn(Optional.of(u));
        return u;
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        storedUser(true);
        AuthResponse r = service(true, true, "").login(new LoginRequest("HR@blossom.demo ", "Demo@12345"));
        assertThat(r.token()).isNotBlank();
        assertThat(r.user().role()).isEqualTo(Role.HR);
    }

    @Test
    void loginRejectsWrongPasswordAndUnknownEmail() {
        storedUser(true);
        assertThatThrownBy(() -> service(true, true, "").login(new LoginRequest("hr@blossom.demo", "nope")))
                .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> service(true, true, "").login(new LoginRequest("ghost@blossom.demo", "x")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginExplainsWhenAccountIsNotApprovedYet() {
        storedUser(false);
        assertThatThrownBy(() -> service(true, true, "").login(new LoginRequest("hr@blossom.demo", "Demo@12345")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("approval");
    }
}
