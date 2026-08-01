package com.hutech.tama.dto.request;

import com.hutech.tama.enums.BackgroundType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BoardRequest(
        @NotBlank(message = "Board title is required")
        @Size(max = 100, message = "Board title must not exceed 100 characters")
        String title,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @NotNull(message = "Background type is required")
        BackgroundType backgroundType,

        @Pattern(
                regexp = "^#[0-9A-Fa-f]{6}$",
                message = "Background color must use the #RRGGBB format"
        )
        String backgroundColor,

        @Size(max = 500, message = "Background image must not exceed 500 characters")
        String backgroundImage
) {
    @AssertTrue(message = "A color Board requires only a color, and an image Board requires only an image")
    public boolean isBackgroundValid() {
        if (backgroundType == null) {
            return true;
        }
        boolean hasColor = backgroundColor != null && !backgroundColor.isBlank();
        boolean hasImage = backgroundImage != null && !backgroundImage.isBlank();
        return switch (backgroundType) {
            case COLOR -> hasColor && !hasImage;
            case IMAGE -> hasImage && !hasColor;
        };
    }
}
