package com.hutech.tama.repository;

import com.hutech.tama.entity.Card;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {

    @EntityGraph(attributePaths = "labels")
    List<Card> findAllByBoardListIdOrderByPositionAscIdAsc(Long boardListId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select card
            from Card card
            where card.boardList.id = :listId
            order by card.position, card.id
            """)
    List<Card> findAllByBoardListIdOrderByPositionAscIdAscForUpdate(
            @Param("listId") Long listId
    );

    @EntityGraph(attributePaths = "labels")
    Optional<Card> findByIdAndBoardListBoardOwnerId(Long id, Long ownerId);

    @Query("""
            select card.boardList.id
            from Card card
            where card.id = :cardId and card.boardList.board.owner.id = :ownerId
            """)
    Optional<Long> findOwnedBoardListId(
            @Param("cardId") Long cardId,
            @Param("ownerId") Long ownerId
    );
}
