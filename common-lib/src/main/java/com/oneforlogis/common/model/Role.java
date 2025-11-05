package com.oneforlogis.common.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {
    MASTER("ROLE_MASTER", "마스터 관리자"),
    HUB_MANAGER("ROLE_HUB_MANAGER", "허브 관리자"),
    DELIVERY_MANAGER("ROLE_DELIVERY_MANAGER", "배송 관리자"),
    COMPANY_MANAGER("ROLE_COMPANY_MANAGER", "업체 관리자");

    private final String key;
    private final String description;

    public static Role fromKey(String key) {
        for (Role role : values()) {
            if (role.key.equals(key)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role key: " + key);
    }

    public static Role fromName(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown role name: " + name);
        }
    }

    public String getAuthority() {
        return this.key;
    }
}