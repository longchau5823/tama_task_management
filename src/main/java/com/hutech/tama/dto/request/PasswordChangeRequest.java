package com.hutech.tama.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PasswordChangeRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 72, message = "New password must contain between 8 and 72 characters")
        String newPassword,

        @NotBlank(message = "Password confirmation is required")
        @Size(max = 72, message = "Password confirmation must not exceed 72 characters")
        String confirmPassword
) {
}
