package com.hutech.tama.service;

import com.hutech.tama.dto.request.BoardRequest;
import com.hutech.tama.dto.response.BoardResponse;
import com.hutech.tama.entity.Board;
import com.hutech.tama.entity.User;
import com.hutech.tama.enums.BackgroundType;
import com.hutech.tama.exception.ForbiddenOperationException;
import com.hutech.tama.exception.InvalidBoardBackgroundException;
import com.hutech.tama.exception.ResourceNotFoundException;
import com.hutech.tama.repository.BoardRepository;
import com.hutech.tama.repository.UserRepository;
import com.hutech.tama.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BoardService {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final BoardBackgroundService boardBackgroundService;

    public BoardService(
            BoardRepository boardRepository,
            UserRepository userRepository,
            BoardBackgroundService boardBackgroundService
    ) {
        this.boardRepository = boardRepository;
        this.userRepository = userRepository;
        this.boardBackgroundService = boardBackgroundService;
    }

    @Transactional(readOnly = true)
    public List<BoardResponse> getBoards() {
        User owner = getCurrentUser();
        return boardRepository.findAllByOwnerIdOrderByUpdatedAtDesc(owner.getId()).stream()
                .map(BoardResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BoardResponse getBoard(Long boardId) {
        User owner = getCurrentUser();
        return BoardResponse.from(getOwnedBoard(boardId, owner.getId()));
    }

    @Transactional
    public BoardResponse createBoard(BoardRequest request) {
        User owner = getCurrentUser();

        Board board = new Board();
        board.setOwner(owner);
        applyAllowedChanges(board, request, owner.getId(), null);

        return BoardResponse.from(boardRepository.saveAndFlush(board));
    }

    @Transactional
    public BoardResponse updateBoard(Long boardId, BoardRequest request) {
        User owner = getCurrentUser();
        Board board = getOwnedBoard(boardId, owner.getId());
        String previousBackgroundImage = board.getBackgroundImage();
        applyAllowedChanges(board, request, owner.getId(), previousBackgroundImage);

        Board savedBoard = boardRepository.saveAndFlush(board);
        if (previousBackgroundImage != null
                && !previousBackgroundImage.equals(savedBoard.getBackgroundImage())) {
            boardBackgroundService.deleteAfterCommit(owner.getId(), previousBackgroundImage);
        }
        return BoardResponse.from(savedBoard);
    }

    @Transactional
    public void deleteBoard(Long boardId) {
        User owner = getCurrentUser();
        Board board = getOwnedBoard(boardId, owner.getId());
        String previousBackgroundImage = board.getBackgroundImage();
        boardRepository.delete(board);
        boardRepository.flush();
        boardBackgroundService.deleteAfterCommit(owner.getId(), previousBackgroundImage);
    }

    private User getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new ForbiddenOperationException("Authentication is required"));
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user no longer exists"));
    }

    private Board getOwnedBoard(Long boardId, Long ownerId) {
        return boardRepository.findByIdAndOwnerId(boardId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found"));
    }

    private void applyAllowedChanges(
            Board board,
            BoardRequest request,
            Long ownerId,
            String existingBackgroundImage
    ) {
        board.setTitle(request.title().trim());
        board.setDescription(trimToNull(request.description()));
        board.setBackgroundType(request.backgroundType());

        if (request.backgroundType() == BackgroundType.COLOR) {
            board.setBackgroundColor(request.backgroundColor().trim().toUpperCase());
            board.setBackgroundImage(null);
        } else {
            String requestedBackgroundImage = request.backgroundImage().trim();
            boolean unchangedLegacyBackground = existingBackgroundImage != null
                    && existingBackgroundImage.equals(requestedBackgroundImage);
            if (!unchangedLegacyBackground
                    && !boardBackgroundService.isAllowedBackground(
                            ownerId,
                            requestedBackgroundImage
                    )) {
                throw new InvalidBoardBackgroundException(
                        "Ảnh nền phải được chọn từ thư viện hoặc tải lên từ máy"
                );
            }
            board.setBackgroundColor(null);
            board.setBackgroundImage(requestedBackgroundImage);
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
