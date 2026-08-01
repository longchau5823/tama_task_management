package com.hutech.tama.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LabelUpdateRequest(
        @NotBlank(message = "Label name is required")
        @Size(max = 50, message = "Label name must not exceed 50 characters")
        String name,

        @NotBlank(message = "Label color is required")
        @Pattern(
                regexp = "^#[0-9A-Fa-f]{6}$",
                message = "Label color must use the #RRGGBB format"
        )
        String color
) {
    public LabelUpdateRequest {
        name = name == null ? null : name.trim();
        color = color == null ? null : color.trim();
    }
}
