package com.hutech.tama.dto.response;

import com.hutech.tama.entity.BoardList;

import java.time.LocalDateTime;

public record BoardListResponse(
        Long id,
        Long boardId,
        String title,
        Integer position,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static BoardListResponse from(BoardList boardList) {
        return new BoardListResponse(
                boardList.getId(),
                boardList.getBoard().getId(),
                boardList.getTitle(),
                boardList.getPosition(),
                boardList.getCreatedAt(),
                boardList.getUpdatedAt()
        );
    }
}
