package io.blossom.erp.controller;

import io.blossom.erp.domain.Role;
import io.blossom.erp.dto.DashboardResponse;
import io.blossom.erp.security.AuthUser;
import io.blossom.erp.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping
    public DashboardResponse stats(@AuthenticationPrincipal AuthUser user) {
        return dashboard.stats(user.hasAnyRole(Role.ADMIN, Role.HR));
    }
}
