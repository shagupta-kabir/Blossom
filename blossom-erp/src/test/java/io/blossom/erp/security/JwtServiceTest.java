package io.blossom.erp.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.blossom.erp.domain.AppUser;
import io.blossom.erp.domain.Role;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-1234";

    private static AppUser user(Role role, String employeeId) {
        AppUser u = new AppUser();
        u.setId("user-1");
        u.setEmail("hr@blossom.demo");
        u.setFullName("Nadia Islam");
        u.setRole(role);
        u.setEmployeeId(employeeId);
        return u;
    }

    @Test
    void tokenRoundTripKeepsIdentityAndRole() {
        JwtService jwt = new JwtService(SECRET, 60);
        AuthUser parsed = jwt.parse(jwt.generate(user(Role.HR, "emp-9")));

        assertThat(parsed.id()).isEqualTo("user-1");
        assertThat(parsed.email()).isEqualTo("hr@blossom.demo");
        assertThat(parsed.fullName()).isEqualTo("Nadia Islam");
        assertThat(parsed.role()).isEqualTo(Role.HR);
        assertThat(parsed.employeeId()).isEqualTo("emp-9");
    }

    @Test
    void tokenWithoutLinkedEmployeeParsesWithNullEmployeeId() {
        JwtService jwt = new JwtService(SECRET, 60);
        assertThat(jwt.parse(jwt.generate(user(Role.ADMIN, null))).employeeId()).isNull();
    }

    @Test
    void rejectsTooShortSecret() {
        assertThatThrownBy(() -> new JwtService("too-short", 60)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void missingSecretFallsBackToWorkingRandomKey() {
        JwtService jwt = new JwtService("", 60);
        AuthUser parsed = jwt.parse(jwt.generate(user(Role.ADMIN, null)));
        assertThat(parsed.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        String token = new JwtService("another-secret-another-secret-another-1", 60).generate(user(Role.ADMIN, null));
        JwtService jwt = new JwtService(SECRET, 60);
        assertThatThrownBy(() -> jwt.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTamperedToken() {
        JwtService jwt = new JwtService(SECRET, 60);
        String token = jwt.generate(user(Role.EMPLOYEE, "e1"));
        String tampered = token.substring(0, token.length() - 4) + "AAAA";
        assertThatThrownBy(() -> jwt.parse(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredToken() {
        JwtService jwt = new JwtService(SECRET, -5);
        String token = jwt.generate(user(Role.EMPLOYEE, "e1"));
        assertThatThrownBy(() -> jwt.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void exposesLifetimeInSeconds() {
        assertThat(new JwtService(SECRET, 30).expiresInSeconds()).isEqualTo(1800);
    }
}
