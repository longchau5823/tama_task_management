package com.hutech.tama.controller.api;

import com.hutech.tama.dto.request.CommentCreateRequest;
import com.hutech.tama.dto.request.CommentUpdateRequest;
import com.hutech.tama.dto.response.CommentResponse;
import com.hutech.tama.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/cards/{cardId}/comments")
    public List<CommentResponse> getComments(@PathVariable Long cardId) {
        return commentService.getComments(cardId);
    }

    @PostMapping("/cards/{cardId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse createComment(@PathVariable Long cardId, @Valid @RequestBody CommentCreateRequest request ) {
        return commentService.createComment(cardId, request);
    }

    @PutMapping("/comments/{commentId}")
    public CommentResponse updateComment(@PathVariable Long commentId, @Valid @RequestBody CommentUpdateRequest request) {
        return commentService.updateComment(commentId, request);
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
    }
}
