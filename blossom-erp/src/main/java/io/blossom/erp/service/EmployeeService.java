package io.blossom.erp.service;

import io.blossom.erp.domain.Department;
import io.blossom.erp.domain.Employee;
import io.blossom.erp.domain.EmployeeStatus;
import io.blossom.erp.dto.OrgDtos.EmployeeRequest;
import io.blossom.erp.dto.OrgDtos.EmployeeResponse;
import io.blossom.erp.dto.PageResponse;
import io.blossom.erp.exception.BadRequestException;
import io.blossom.erp.exception.ConflictException;
import io.blossom.erp.exception.ResourceNotFoundException;
import io.blossom.erp.repository.DepartmentRepository;
import io.blossom.erp.repository.EmployeeRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmployeeService {

    private final EmployeeRepository employees;
    private final DepartmentRepository departments;
    private final MongoTemplate mongo;

    public EmployeeService(EmployeeRepository employees, DepartmentRepository departments, MongoTemplate mongo) {
        this.employees = employees;
        this.departments = departments;
        this.mongo = mongo;
    }

    public PageResponse<EmployeeResponse> search(String q, String departmentId, EmployeeStatus status, Pageable pageable,
                                                 boolean includeSalary) {
        List<Criteria> filters = new ArrayList<>();
        if (StringUtils.hasText(q)) {
            String rx = Pattern.quote(q.trim());
            filters.add(new Criteria().orOperator(
                    Criteria.where("firstName").regex(rx, "i"),
                    Criteria.where("lastName").regex(rx, "i"),
                    Criteria.where("email").regex(rx, "i"),
                    Criteria.where("jobTitle").regex(rx, "i")));
        }
        if (StringUtils.hasText(departmentId)) {
            filters.add(Criteria.where("departmentId").is(departmentId));
        }
        if (status != null) {
            filters.add(Criteria.where("status").is(status));
        }
        Query query = new Query();
        if (!filters.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(filters.toArray(new Criteria[0])));
        }
        long total = mongo.count(query, Employee.class);
        List<Employee> rows = mongo.find(query.with(pageable), Employee.class);
        Map<String, String> deptNames = departmentNames();
        List<EmployeeResponse> content = rows.stream().map(e -> toResponse(e, deptNames, includeSalary)).toList();
        return PageResponse.of(content, pageable.getPageNumber(), pageable.getPageSize(), total);
    }

    public EmployeeResponse get(String id, boolean includeSalary) {
        return toResponse(find(id), departmentNames(), includeSalary);
    }

    public EmployeeResponse create(EmployeeRequest req) {
        String email = req.email().trim().toLowerCase();
        if (employees.existsByEmail(email)) {
            throw new ConflictException("An employee with this email already exists");
        }
        Employee e = new Employee();
        e.setCreatedAt(Instant.now());
        apply(e, req, email);
        return toResponse(employees.save(e), departmentNames(), true);
    }

    public EmployeeResponse update(String id, EmployeeRequest req) {
        Employee e = find(id);
        String email = req.email().trim().toLowerCase();
        if (!email.equals(e.getEmail()) && employees.existsByEmail(email)) {
            throw new ConflictException("An employee with this email already exists");
        }
        if (id.equals(req.managerId())) {
            throw new BadRequestException("An employee cannot be their own manager");
        }
        apply(e, req, email);
        return toResponse(employees.save(e), departmentNames(), true);
    }

    public void delete(String id) {
        Employee e = find(id);
        if (employees.existsByManagerId(id)) {
            throw new ConflictException("This employee manages other employees; reassign them first");
        }
        employees.delete(e);
    }

    private void apply(Employee e, EmployeeRequest req, String email) {
        if (StringUtils.hasText(req.departmentId()) && !departments.existsById(req.departmentId())) {
            throw new ResourceNotFoundException("Department", req.departmentId());
        }
        if (StringUtils.hasText(req.managerId()) && !employees.existsById(req.managerId())) {
            throw new ResourceNotFoundException("Employee (manager)", req.managerId());
        }
        e.setFirstName(req.firstName().trim());
        e.setLastName(req.lastName().trim());
        e.setEmail(email);
        e.setPhone(req.phone());
        e.setJobTitle(req.jobTitle().trim());
        e.setDepartmentId(StringUtils.hasText(req.departmentId()) ? req.departmentId() : null);
        e.setManagerId(StringUtils.hasText(req.managerId()) ? req.managerId() : null);
        e.setHireDate(req.hireDate());
        e.setStatus(req.status() == null ? EmployeeStatus.ACTIVE : req.status());
        e.setCity(StringUtils.hasText(req.city()) ? req.city().trim() : null);
        e.setMonthlySalary(req.monthlySalary());
    }

    private Employee find(String id) {
        return employees.findById(id).orElseThrow(() -> new ResourceNotFoundException("Employee", id));
    }

    private Map<String, String> departmentNames() {
        return departments.findAll().stream().collect(Collectors.toMap(Department::getId, Department::getName, (a, b) -> a));
    }

    /** Salary is confidential: only ADMIN and HR callers get it (includeSalary = true). */
    private EmployeeResponse toResponse(Employee e, Map<String, String> deptNames, boolean includeSalary) {
        return new EmployeeResponse(e.getId(), e.getFirstName(), e.getLastName(), e.getFirstName() + " " + e.getLastName(),
                e.getEmail(), e.getPhone(), e.getJobTitle(), e.getDepartmentId(),
                e.getDepartmentId() == null ? null : deptNames.get(e.getDepartmentId()),
                e.getManagerId(), e.getHireDate(), e.getStatus(), e.getCity(), includeSalary ? e.getMonthlySalary() : null);
    }
}
