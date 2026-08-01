package com.hutech.tama.dto.response;

import com.hutech.tama.entity.Card;
import com.hutech.tama.enums.CardPriority;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record CardResponse(
        Long id,
        Long listId,
        String title,
        String description,
        Integer position,
        LocalDateTime dueDate,
        Boolean completed,
        LocalDateTime completedAt,
        CardPriority priority,
        List<LabelSummaryResponse> labels,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CardResponse from(Card card) {
        return new CardResponse(
                card.getId(),
                card.getBoardList().getId(),
                card.getTitle(),
                card.getDescription(),
                card.getPosition(),
                card.getDueDate(),
                card.getCompleted(),
                card.getCompletedAt(),
                card.getPriority(),
                card.getLabels().stream()
                        .map(LabelSummaryResponse::from)
                        .sorted(Comparator.comparing(
                                LabelSummaryResponse::name,
                                String.CASE_INSENSITIVE_ORDER
                        ).thenComparing(LabelSummaryResponse::id))
                        .toList(),
                card.getCreatedAt(),
                card.getUpdatedAt()
        );
    }
}
