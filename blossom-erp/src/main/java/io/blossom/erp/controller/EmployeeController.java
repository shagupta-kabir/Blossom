package io.blossom.erp.controller;

import io.blossom.erp.domain.EmployeeStatus;
import io.blossom.erp.dto.OrgDtos.EmployeeRequest;
import io.blossom.erp.dto.OrgDtos.EmployeeResponse;
import io.blossom.erp.domain.Role;
import io.blossom.erp.dto.PageResponse;
import io.blossom.erp.security.AuthUser;
import io.blossom.erp.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employees;

    public EmployeeController(EmployeeService employees) {
        this.employees = employees;
    }

    @GetMapping
    public PageResponse<EmployeeResponse> search(@RequestParam(required = false) String q,
                                                 @RequestParam(required = false) String departmentId,
                                                 @RequestParam(required = false) EmployeeStatus status,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size,
                                                 @AuthenticationPrincipal AuthUser user) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        return employees.search(q, departmentId, status, PageRequest.of(safePage, safeSize, Sort.by("lastName", "firstName")),
                user.hasAnyRole(Role.ADMIN, Role.HR));
    }

    @GetMapping("/{id}")
    public EmployeeResponse get(@PathVariable String id, @AuthenticationPrincipal AuthUser user) {
        return employees.get(id, user.hasAnyRole(Role.ADMIN, Role.HR));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse create(@Valid @RequestBody EmployeeRequest req) {
        return employees.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public EmployeeResponse update(@PathVariable String id, @Valid @RequestBody EmployeeRequest req) {
        return employees.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        employees.delete(id);
    }
}
