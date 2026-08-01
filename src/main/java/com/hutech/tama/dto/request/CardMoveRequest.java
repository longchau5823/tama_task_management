package com.hutech.tama.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CardMoveRequest(
        @NotNull(message = "Target BoardList is required")
        Long targetListId,

        @NotNull(message = "Target position is required")
        @Min(value = 0, message = "Target position must not be negative")
        Integer targetPosition
) {
}
