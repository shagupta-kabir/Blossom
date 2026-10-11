package io.blossom.erp.controller;

import io.blossom.erp.dto.AuthDtos.AuthResponse;
import io.blossom.erp.dto.AuthDtos.ChangePasswordRequest;
import io.blossom.erp.dto.AuthDtos.LoginRequest;
import io.blossom.erp.dto.AuthDtos.RegisterRequest;
import io.blossom.erp.dto.AuthDtos.RegisterResponse;
import io.blossom.erp.dto.AuthDtos.UserResponse;
import io.blossom.erp.security.AuthUser;
import io.blossom.erp.domain.Role;
import io.blossom.erp.exception.BadRequestException;
import io.blossom.erp.service.AuthService;
import io.blossom.erp.service.DemoAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final DemoAccountService demo;

    public AuthController(AuthService auth, DemoAccountService demo) {
        this.auth = auth;
        this.demo = demo;
    }

    public record DemoLoginRequest(String role) {
    }

    /** Demo mode only: one-click sign-in for ADMIN / HR / MANAGER / EMPLOYEE. Answers 403 when demo mode is off. */
    @PostMapping("/demo-login")
    public AuthResponse demoLogin(@RequestBody DemoLoginRequest req) {
        Role role;
        try {
            role = Role.valueOf(req.role() == null ? "" : req.role().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unknown role. Use ADMIN, HR, MANAGER or EMPLOYEE.");
        }
        return demo.login(role);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest req) {
        return auth.register(req);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthUser user) {
        return auth.me(user.id());
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody ChangePasswordRequest req) {
        auth.changePassword(user.id(), req);
    }
}
