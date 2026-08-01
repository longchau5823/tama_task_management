package com.hutech.tama.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateAdminRoleRequest(
        @NotNull Boolean admin
) {
}
