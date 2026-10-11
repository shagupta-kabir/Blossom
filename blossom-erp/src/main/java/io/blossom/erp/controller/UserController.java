package io.blossom.erp.controller;

import io.blossom.erp.dto.AuthDtos.CreateUserRequest;
import io.blossom.erp.dto.AuthDtos.UpdateUserRequest;
import io.blossom.erp.dto.AuthDtos.UserResponse;
import io.blossom.erp.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping
    public List<UserResponse> list() {
        return users.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest req) {
        return users.create(req);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable String id, @RequestBody UpdateUserRequest req) {
        return users.update(id, req);
    }
}
