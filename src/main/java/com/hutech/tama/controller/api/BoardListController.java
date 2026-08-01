package com.hutech.tama.controller.api;

import com.hutech.tama.dto.request.BoardListReorderRequest;
import com.hutech.tama.dto.request.BoardListRequest;
import com.hutech.tama.dto.response.BoardListResponse;
import com.hutech.tama.service.BoardListService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
public class BoardListController {

    private final BoardListService boardListService;

    public BoardListController(BoardListService boardListService) {
        this.boardListService = boardListService;
    }

    @GetMapping("/boards/{boardId}/lists")
    public List<BoardListResponse> getBoardLists(@PathVariable Long boardId) {
        return boardListService.getBoardLists(boardId);
    }

    @GetMapping("/lists/{listId}")
    public BoardListResponse getBoardList(@PathVariable Long listId) {
        return boardListService.getBoardList(listId);
    }

    @PostMapping("/boards/{boardId}/lists")
    @ResponseStatus(HttpStatus.CREATED)
    public BoardListResponse createBoardList(@PathVariable Long boardId, @Valid @RequestBody BoardListRequest request) {
        return boardListService.createBoardList(boardId, request);
    }

    @PutMapping("/lists/{listId}")
    public BoardListResponse updateBoardList(@PathVariable Long listId, @Valid @RequestBody BoardListRequest request) {
        return boardListService.updateBoardList(listId, request);
    }

    @DeleteMapping("/lists/{listId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBoardList(@PathVariable Long listId) {
        boardListService.deleteBoardList(listId);
    }

    @PatchMapping("/boards/{boardId}/lists/reorder")
    public List<BoardListResponse> reorderBoardLists(@PathVariable Long boardId, @Valid @RequestBody BoardListReorderRequest request) {
        return boardListService.reorderBoardLists(boardId, request);
    }
}
