package com.hutech.tama.service;

import com.hutech.tama.dto.request.CommentCreateRequest;
import com.hutech.tama.dto.request.CommentUpdateRequest;
import com.hutech.tama.dto.response.CommentResponse;
import com.hutech.tama.entity.Card;
import com.hutech.tama.entity.Comment;
import com.hutech.tama.entity.User;
import com.hutech.tama.exception.ForbiddenOperationException;
import com.hutech.tama.exception.ResourceNotFoundException;
import com.hutech.tama.repository.CardRepository;
import com.hutech.tama.repository.CommentRepository;
import com.hutech.tama.repository.UserRepository;
import com.hutech.tama.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;

    public CommentService(
            CommentRepository commentRepository,
            CardRepository cardRepository,
            UserRepository userRepository
    ) {
        this.commentRepository = commentRepository;
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long cardId) {
        User currentUser = getCurrentUser();
        getOwnedCard(cardId, currentUser.getId());
        return commentRepository.findAllByCardIdOrderByCreatedAtAscIdAsc(cardId).stream()
                .map(CommentResponse::from)
                .toList();
    }

    @Transactional
    public CommentResponse createComment(Long cardId, CommentCreateRequest request) {
        User currentUser = getCurrentUser();
        Card card = getOwnedCard(cardId, currentUser.getId());

        Comment comment = new Comment();
        comment.setCard(card);
        comment.setAuthor(currentUser);
        comment.setContent(request.content().trim());
        return CommentResponse.from(commentRepository.saveAndFlush(comment));
    }

    @Transactional
    public CommentResponse updateComment(Long commentId, CommentUpdateRequest request) {
        User currentUser = getCurrentUser();
        Comment comment = getOwnedComment(commentId, currentUser.getId());
        String normalizedContent = request.content().trim();

        if (!comment.getContent().equals(normalizedContent)) {
            comment.setContent(normalizedContent);
            commentRepository.saveAndFlush(comment);
        }
        return CommentResponse.from(comment);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        User currentUser = getCurrentUser();
        Comment comment = getOwnedComment(commentId, currentUser.getId());
        commentRepository.delete(comment);
        commentRepository.flush();
    }

    private User getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new ForbiddenOperationException("Authentication is required"));
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authenticated user no longer exists"
                ));
    }

    private Card getOwnedCard(Long cardId, Long ownerId) {
        return cardRepository.findByIdAndBoardListBoardOwnerId(cardId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy thẻ công việc"
                ));
    }

    private Comment getOwnedComment(Long commentId, Long currentUserId) {
        return commentRepository
                .findByIdAndAuthorIdAndCardBoardListBoardOwnerId(
                        commentId,
                        currentUserId,
                        currentUserId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy bình luận"
                ));
    }
}
