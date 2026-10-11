package io.blossom.erp.controller;

import io.blossom.erp.domain.LeaveStatus;
import io.blossom.erp.dto.LeaveDtos.LeaveCreateRequest;
import io.blossom.erp.dto.LeaveDtos.LeaveDecisionRequest;
import io.blossom.erp.dto.LeaveDtos.LeaveResponse;
import io.blossom.erp.security.AuthUser;
import io.blossom.erp.service.LeaveService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leave")
public class LeaveController {

    private final LeaveService leave;

    public LeaveController(LeaveService leave) {
        this.leave = leave;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveResponse request(@Valid @RequestBody LeaveCreateRequest req, @AuthenticationPrincipal AuthUser user) {
        return leave.create(req, user);
    }

    @GetMapping("/mine")
    public List<LeaveResponse> mine(@AuthenticationPrincipal AuthUser user) {
        return leave.mine(user);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public List<LeaveResponse> review(@RequestParam(required = false) LeaveStatus status,
                                      @AuthenticationPrincipal AuthUser user) {
        return leave.review(user, status);
    }

    @PatchMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public LeaveResponse decide(@PathVariable String id, @Valid @RequestBody LeaveDecisionRequest req,
                                @AuthenticationPrincipal AuthUser user) {
        return leave.decide(id, req, user);
    }

    @PostMapping("/{id}/cancel")
    public LeaveResponse cancel(@PathVariable String id, @AuthenticationPrincipal AuthUser user) {
        return leave.cancel(id, user);
    }
}
