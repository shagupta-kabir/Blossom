package io.blossom.erp.service;

import io.blossom.erp.domain.Department;
import io.blossom.erp.dto.OrgDtos.DepartmentRequest;
import io.blossom.erp.dto.OrgDtos.DepartmentResponse;
import io.blossom.erp.exception.ConflictException;
import io.blossom.erp.exception.ResourceNotFoundException;
import io.blossom.erp.repository.DepartmentRepository;
import io.blossom.erp.repository.EmployeeRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DepartmentService {

    private final DepartmentRepository departments;
    private final EmployeeRepository employees;

    public DepartmentService(DepartmentRepository departments, EmployeeRepository employees) {
        this.departments = departments;
        this.employees = employees;
    }

    public List<DepartmentResponse> list() {
        return departments.findAll().stream()
                .sorted(Comparator.comparing(Department::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse).toList();
    }

    public DepartmentResponse get(String id) {
        return toResponse(find(id));
    }

    public DepartmentResponse create(DepartmentRequest req) {
        String name = req.name().trim();
        if (departments.existsByNameIgnoreCase(name)) {
            throw new ConflictException("A department named '" + name + "' already exists");
        }
        Department d = new Department();
        d.setCreatedAt(Instant.now());
        apply(d, req, name);
        return toResponse(departments.save(d));
    }

    public DepartmentResponse update(String id, DepartmentRequest req) {
        Department d = find(id);
        String name = req.name().trim();
        if (!d.getName().equalsIgnoreCase(name) && departments.existsByNameIgnoreCase(name)) {
            throw new ConflictException("A department named '" + name + "' already exists");
        }
        apply(d, req, name);
        return toResponse(departments.save(d));
    }

    public void delete(String id) {
        Department d = find(id);
        if (employees.countByDepartmentId(id) > 0) {
            throw new ConflictException("Department still has employees; move them before deleting it");
        }
        departments.delete(d);
    }

    private void apply(Department d, DepartmentRequest req, String name) {
        if (StringUtils.hasText(req.headEmployeeId()) && !employees.existsById(req.headEmployeeId())) {
            throw new ResourceNotFoundException("Employee", req.headEmployeeId());
        }
        d.setName(name);
        d.setDescription(req.description());
        d.setHeadEmployeeId(StringUtils.hasText(req.headEmployeeId()) ? req.headEmployeeId() : null);
    }

    private Department find(String id) {
        return departments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Department", id));
    }

    private DepartmentResponse toResponse(Department d) {
        String headName = d.getHeadEmployeeId() == null ? null
                : employees.findById(d.getHeadEmployeeId()).map(e -> e.getFirstName() + " " + e.getLastName()).orElse(null);
        return new DepartmentResponse(d.getId(), d.getName(), d.getDescription(), d.getHeadEmployeeId(), headName,
                employees.countByDepartmentId(d.getId()));
    }
}
