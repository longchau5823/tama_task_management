package com.hutech.tama.repository;

import com.hutech.tama.entity.BoardList;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BoardListRepository extends JpaRepository<BoardList, Long> {

    List<BoardList> findAllByBoardIdOrderByPositionAsc(Long boardId);

    Optional<BoardList> findByIdAndBoardOwnerId(Long id, Long ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select boardList
            from BoardList boardList
            where boardList.board.id = :boardId
            order by boardList.position, boardList.id
            """)
    List<BoardList> findAllByBoardIdOrderByPositionAscForUpdate(
            @Param("boardId") Long boardId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select boardList
            from BoardList boardList
            where boardList.id in :listIds and boardList.board.owner.id = :ownerId
            order by boardList.id
            """)
    List<BoardList> findAllOwnedByIdInForUpdate(
            @Param("listIds") List<Long> listIds,
            @Param("ownerId") Long ownerId
    );
}
