package io.blossom.erp.security;

import io.blossom.erp.domain.Role;

/** The authenticated principal rebuilt from JWT claims on every request (stateless). */
public record AuthUser(String id, String email, String fullName, Role role, String employeeId) {

    public boolean hasAnyRole(Role... roles) {
        for (Role r : roles) {
            if (r == role) {
                return true;
            }
        }
        return false;
    }
}
