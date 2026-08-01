package com.hutech.tama.dto.request;

import jakarta.validation.constraints.NotNull;

public record CardCompletionRequest(
        @NotNull(message = "Completed status is required")
        Boolean completed
) {
}
