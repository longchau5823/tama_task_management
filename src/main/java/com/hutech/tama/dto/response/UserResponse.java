package com.hutech.tama.dto.response;

import com.hutech.tama.enums.RoleName;
import com.hutech.tama.entity.Role;
import com.hutech.tama.entity.User;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public record UserResponse(
        Long id,
        String username,
        String email,
        Boolean enabled,
        Set<RoleName> roles,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String avatarPath
) {
    public UserResponse(
            Long id,
            String username,
            String email,
            Boolean enabled,
            Set<RoleName> roles,
            LocalDateTime createdAt
    ) {
        this(id, username, email, enabled, roles, createdAt, null, User.DEFAULT_AVATAR_PATH);
    }

    public UserResponse(
            Long id,
            String username,
            String email,
            Boolean enabled,
            Set<RoleName> roles,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this(id, username, email, enabled, roles, createdAt, updatedAt, User.DEFAULT_AVATAR_PATH);
    }

    public static UserResponse from(User user) {
        Set<RoleName> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toUnmodifiableSet());
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getEnabled(),
                roles,
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getAvatarPath()
        );
    }
}
