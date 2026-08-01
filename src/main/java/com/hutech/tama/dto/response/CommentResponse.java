package com.hutech.tama.dto.response;

import com.hutech.tama.entity.Comment;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record CommentResponse(
        Long id,
        Long cardId,
        Long authorId,
        String authorUsername,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean edited
) {
    public static CommentResponse from(Comment comment) {
        LocalDateTime createdAt = comment.getCreatedAt();
        LocalDateTime updatedAt = comment.getUpdatedAt();
        return new CommentResponse(
                comment.getId(),
                comment.getCard().getId(),
                comment.getAuthor().getId(),
                comment.getAuthor().getUsername(),
                comment.getContent(),
                createdAt,
                updatedAt,
                createdAt != null
                        && updatedAt != null
                        && updatedAt.truncatedTo(ChronoUnit.SECONDS)
                                .isAfter(createdAt.truncatedTo(ChronoUnit.SECONDS))
        );
    }
}
