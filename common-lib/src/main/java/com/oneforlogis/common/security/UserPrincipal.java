package com.oneforlogis.common.security;

import com.oneforlogis.common.model.Role;
import java.util.UUID;

public record UserPrincipal(
        UUID id,
        String username,
        Role role
){

    public boolean isMaster() {
        return this.role == Role.MASTER;
    }

    public boolean hasRole(Role targetRole) {
        return this.role == targetRole;
    }

    public String getRoleKey() {
        return this.role.getKey();
    }
}