package com.hutech.tama.repository;

import com.hutech.tama.entity.Comment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = "author")
    List<Comment> findAllByCardIdOrderByCreatedAtAscIdAsc(Long cardId);

    @EntityGraph(attributePaths = {"author", "card"})
    Optional<Comment> findByIdAndAuthorIdAndCardBoardListBoardOwnerId(
            Long id,
            Long authorId,
            Long ownerId
    );
}
