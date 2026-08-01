package com.hutech.tama.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CommentUpdateRequest(
        @NotBlank(message = "Nội dung bình luận không được để trống")
        String content
) {
    public CommentUpdateRequest {
        content = content == null ? null : content.trim();
    }
}
