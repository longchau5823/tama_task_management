package com.hutech.tama.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateUserEnabledRequest(
        @NotNull Boolean enabled
) {
}
