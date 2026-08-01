package com.hutech.tama.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BoardListReorderRequest(
        @NotNull(message = "Ordered BoardList IDs are required")
        List<@NotNull(message = "BoardList ID must not be null") Long> orderedListIds
) {
}
