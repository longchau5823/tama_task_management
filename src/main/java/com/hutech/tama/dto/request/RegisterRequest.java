package com.hutech.tama.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(min = 8, max = 72) String password
) {
    public RegisterRequest {
        username = username == null ? null : username.trim();
        email = email == null ? null : email.trim();
    }
}
