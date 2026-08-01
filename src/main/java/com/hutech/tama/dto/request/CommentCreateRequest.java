package com.hutech.tama.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CommentCreateRequest(
        @NotBlank(message = "Nội dung bình luận không được để trống")
        String content
) {
    public CommentCreateRequest {
        content = content == null ? null : content.trim();
    }
}
