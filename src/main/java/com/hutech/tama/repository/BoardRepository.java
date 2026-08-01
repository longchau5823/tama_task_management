package com.hutech.tama.repository;

import com.hutech.tama.entity.Board;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Long> {

    List<Board> findAllByOwnerIdOrderByUpdatedAtDesc(Long ownerId);

    Optional<Board> findByIdAndOwnerId(Long id, Long ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select board
            from Board board
            where board.id = :boardId and board.owner.id = :ownerId
            """)
    Optional<Board> findByIdAndOwnerIdForUpdate(
            @Param("boardId") Long boardId,
            @Param("ownerId") Long ownerId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select board
            from Board board
            join BoardList boardList on boardList.board = board
            where boardList.id = :listId and board.owner.id = :ownerId
            """)
    Optional<Board> findOwnedBoardByListIdForUpdate(
            @Param("listId") Long listId,
            @Param("ownerId") Long ownerId
    );

    boolean existsByOwnerIdAndBackgroundImage(Long ownerId, String backgroundImage);
}
