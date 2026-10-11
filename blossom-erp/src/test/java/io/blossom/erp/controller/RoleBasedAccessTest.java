package io.blossom.erp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.blossom.erp.config.SecurityConfig;
import io.blossom.erp.domain.AppUser;
import io.blossom.erp.domain.EmployeeStatus;
import io.blossom.erp.domain.Role;
import io.blossom.erp.dto.AuthDtos.RegisterResponse;
import io.blossom.erp.dto.DashboardResponse;
import io.blossom.erp.dto.OrgDtos.EmployeeResponse;
import io.blossom.erp.dto.PageResponse;
import io.blossom.erp.exception.ResourceNotFoundException;
import io.blossom.erp.security.JwtService;
import io.blossom.erp.service.AuthService;
import io.blossom.erp.service.DashboardService;
import io.blossom.erp.service.EmployeeService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Exercises the real security chain (JWT filter + @PreAuthorize) and the global exception handler. */
@WebMvcTest(controllers = {EmployeeController.class, DashboardController.class, AuthController.class})
@Import({SecurityConfig.class, JwtService.class})
@TestPropertySource(properties = {"app.jwt.secret=web-test-secret-web-test-secret-web-test-1", "app.cors.allowed-origins=*"})
class RoleBasedAccessTest {

    private static final String VALID_EMPLOYEE = """
            {"firstName":"Ayesha","lastName":"Noor","email":"ayesha@blossom.demo",
             "jobTitle":"Analyst","hireDate":"2026-01-15"}""";

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwt;

    @MockitoBean
    EmployeeService employeeService;
    @MockitoBean
    DashboardService dashboardService;
    @MockitoBean
    AuthService authService;

    private String bearer(Role role) {
        AppUser u = new AppUser();
        u.setId("user-1");
        u.setEmail("test@blossom.demo");
        u.setFullName("Test User");
        u.setRole(role);
        u.setEmployeeId("e1");
        return "Bearer " + jwt.generate(u);
    }

    @Test
    void signUpIsPublicAndReturnsCreated() throws Exception {
        when(authService.register(any())).thenReturn(new RegisterResponse("Account created.", false, null));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Abony\",\"lastName\":\"Rahman\",\"email\":\"abony@example.com\",\"password\":\"Secret#123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void signUpValidatesPasswordLengthAndEmail() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"A\",\"lastName\":\"B\",\"email\":\"bad\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void requestWithoutTokenIsUnauthorized() throws Exception {
        mvc.perform(get("/api/employees"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void garbageTokenIsUnauthorized() throws Exception {
        mvc.perform(get("/api/employees").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anyAuthenticatedRoleCanListEmployees() throws Exception {
        when(employeeService.search(any(), any(), any(), any(), anyBoolean())).thenReturn(PageResponse.of(List.of(), 0, 20, 0));

        mvc.perform(get("/api/employees").header("Authorization", bearer(Role.EMPLOYEE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void employeeRoleCannotCreateEmployees() throws Exception {
        mvc.perform(post("/api/employees").header("Authorization", bearer(Role.EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_EMPLOYEE))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerRoleCannotCreateEmployees() throws Exception {
        mvc.perform(post("/api/employees").header("Authorization", bearer(Role.MANAGER))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_EMPLOYEE))
                .andExpect(status().isForbidden());
    }

    @Test
    void hrCanCreateEmployees() throws Exception {
        when(employeeService.create(any())).thenReturn(new EmployeeResponse("e9", "Ayesha", "Noor", "Ayesha Noor",
                "ayesha@blossom.demo", null, "Analyst", null, null, null, LocalDate.of(2026, 1, 15), EmployeeStatus.ACTIVE, "Dhaka", 85000L));

        mvc.perform(post("/api/employees").header("Authorization", bearer(Role.HR))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_EMPLOYEE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ayesha@blossom.demo"));
    }

    @Test
    void invalidBodyReturnsFieldErrors() throws Exception {
        mvc.perform(post("/api/employees").header("Authorization", bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"\",\"lastName\":\"Noor\",\"email\":\"not-an-email\",\"jobTitle\":\"Analyst\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.firstName").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.hireDate").exists());
    }

    @Test
    void phoneMustBeABangladeshiMobileNumber() throws Exception {
        String body = "{\"firstName\":\"Ayesha\",\"lastName\":\"Noor\",\"email\":\"a@blossom.demo\",\"jobTitle\":\"Analyst\","
                + "\"hireDate\":\"2026-01-15\",\"phone\":\"12345\"}";
        mvc.perform(post("/api/employees").header("Authorization", bearer(Role.HR))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.phone").exists());
    }

    @Test
    void negativeSalaryIsRejected() throws Exception {
        String body = "{\"firstName\":\"Ayesha\",\"lastName\":\"Noor\",\"email\":\"a@blossom.demo\",\"jobTitle\":\"Analyst\","
                + "\"hireDate\":\"2026-01-15\",\"monthlySalary\":-5}";
        mvc.perform(post("/api/employees").header("Authorization", bearer(Role.HR))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.monthlySalary").exists());
    }

    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
        mvc.perform(post("/api/employees").header("Authorization", bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{oops"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownEmployeeReturnsNotFoundJson() throws Exception {
        when(employeeService.get(any(), anyBoolean())).thenThrow(new ResourceNotFoundException("Employee", "ghost"));

        mvc.perform(get("/api/employees/ghost").header("Authorization", bearer(Role.EMPLOYEE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee not found: ghost"));
    }

    @Test
    void onlyAdminOrHrCanDeleteEmployees() throws Exception {
        mvc.perform(delete("/api/employees/e1").header("Authorization", bearer(Role.EMPLOYEE)))
                .andExpect(status().isForbidden());

        mvc.perform(delete("/api/employees/e1").header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isNoContent());
        verify(employeeService).delete("e1");
    }

    @Test
    void dashboardRequiresAuthenticationButNotAParticularRole() throws Exception {
        when(dashboardService.stats(anyBoolean())).thenReturn(new DashboardResponse(5, 4, 2, 1, 0, 3, 7, 1,
                "BDT", null, 2_500_000L, Map.of("TODO", 3L), Map.of("ACTIVE", 2L), List.of(), List.of()));

        mvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/dashboard").header("Authorization", bearer(Role.EMPLOYEE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEmployees").value(5))
                .andExpect(jsonPath("$.overdueTasks").value(1))
                .andExpect(jsonPath("$.currency").value("BDT"));
    }
}
