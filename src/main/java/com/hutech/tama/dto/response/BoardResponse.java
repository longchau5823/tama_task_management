package com.hutech.tama.dto.response;

import com.hutech.tama.entity.Board;
import com.hutech.tama.enums.BackgroundType;

import java.time.LocalDateTime;

public record BoardResponse(
        Long id,
        String title,
        String description,
        BackgroundType backgroundType,
        String backgroundColor,
        String backgroundImage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static BoardResponse from(Board board) {
        return new BoardResponse(
                board.getId(),
                board.getTitle(),
                board.getDescription(),
                board.getBackgroundType(),
                board.getBackgroundColor(),
                board.getBackgroundImage(),
                board.getCreatedAt(),
                board.getUpdatedAt()
        );
    }
}
