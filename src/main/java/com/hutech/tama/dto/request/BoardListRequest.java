package com.hutech.tama.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BoardListRequest(
        @NotBlank(message = "BoardList title is required")
        @Size(max = 100, message = "BoardList title must not exceed 100 characters")
        String title
) {
}
