package com.hutech.tama.dto.request;

import com.hutech.tama.enums.CardPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CardUpdateRequest(
        @NotBlank(message = "Card title is required")
        @Size(max = 150, message = "Card title must not exceed 150 characters")
        String title,

        String description,

        LocalDateTime dueDate,

        @NotNull(message = "Priority is required")
        CardPriority priority
) {
}
