package io.blossom.erp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.blossom.erp.domain.Employee;
import io.blossom.erp.domain.EmployeeStatus;
import io.blossom.erp.dto.OrgDtos.EmployeeRequest;
import io.blossom.erp.dto.OrgDtos.EmployeeResponse;
import io.blossom.erp.exception.BadRequestException;
import io.blossom.erp.exception.ConflictException;
import io.blossom.erp.exception.ResourceNotFoundException;
import io.blossom.erp.repository.DepartmentRepository;
import io.blossom.erp.repository.EmployeeRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.mongodb.core.MongoTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmployeeServiceTest {

    @Mock
    EmployeeRepository employees;
    @Mock
    DepartmentRepository departments;
    @Mock
    MongoTemplate mongo;

    EmployeeService service;

    @BeforeEach
    void setUp() {
        service = new EmployeeService(employees, departments, mongo);
        when(employees.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static EmployeeRequest request(String email, String departmentId, String managerId) {
        return new EmployeeRequest("  Ayesha ", "Noor", email, null, "Analyst", departmentId, managerId,
                LocalDate.of(2026, 1, 15), null, "Dhaka", 85_000L);
    }

    @Test
    void createNormalisesEmailAndDefaultsToActive() {
        EmployeeResponse r = service.create(request("  Ayesha.Noor@Blossom.Demo ", null, null));

        assertThat(r.email()).isEqualTo("ayesha.noor@blossom.demo");
        assertThat(r.firstName()).isEqualTo("Ayesha");
        assertThat(r.fullName()).isEqualTo("Ayesha Noor");
        assertThat(r.status()).isEqualTo(EmployeeStatus.ACTIVE);
    }

    @Test
    void createRejectsDuplicateEmail() {
        when(employees.existsByEmail("dup@blossom.demo")).thenReturn(true);
        assertThatThrownBy(() -> service.create(request("dup@blossom.demo", null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createRejectsUnknownDepartment() {
        when(departments.existsById("nope")).thenReturn(false);
        assertThatThrownBy(() -> service.create(request("a@blossom.demo", "nope", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateRejectsSelfAsManager() {
        Employee existing = new Employee();
        existing.setId("e1");
        existing.setEmail("a@blossom.demo");
        when(employees.findById("e1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update("e1", request("a@blossom.demo", null, "e1")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void cannotDeleteEmployeeWhoManagesOthers() {
        Employee boss = new Employee();
        boss.setId("boss");
        when(employees.findById("boss")).thenReturn(Optional.of(boss));
        when(employees.existsByManagerId("boss")).thenReturn(true);

        assertThatThrownBy(() -> service.delete("boss")).isInstanceOf(ConflictException.class);
    }

    @Test
    void salaryAndCityAreStoredInTaka() {
        EmployeeResponse r = service.create(request("a@blossom.demo", null, null));
        assertThat(r.city()).isEqualTo("Dhaka");
        assertThat(r.monthlySalary()).isEqualTo(85_000L);
    }

    @Test
    void salaryIsOnlyVisibleToPrivilegedCallers() {
        Employee e = new Employee();
        e.setId("e1");
        e.setFirstName("A");
        e.setLastName("B");
        e.setMonthlySalary(85_000L);
        when(employees.findById("e1")).thenReturn(Optional.of(e));

        assertThat(service.get("e1", false).monthlySalary()).isNull();
        assertThat(service.get("e1", true).monthlySalary()).isEqualTo(85_000L);
    }

    @Test
    void getUnknownEmployeeIsNotFound() {
        when(employees.findById("ghost")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get("ghost", false)).isInstanceOf(ResourceNotFoundException.class);
    }
}
