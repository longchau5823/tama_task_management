package com.hutech.tama.service;

import com.hutech.tama.dto.request.BoardListReorderRequest;
import com.hutech.tama.dto.request.BoardListRequest;
import com.hutech.tama.dto.response.BoardListResponse;
import com.hutech.tama.entity.Board;
import com.hutech.tama.entity.BoardList;
import com.hutech.tama.entity.User;
import com.hutech.tama.exception.ForbiddenOperationException;
import com.hutech.tama.exception.InvalidMoveException;
import com.hutech.tama.exception.ResourceNotFoundException;
import com.hutech.tama.repository.BoardListRepository;
import com.hutech.tama.repository.BoardRepository;
import com.hutech.tama.repository.UserRepository;
import com.hutech.tama.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BoardListService {

    private final BoardListRepository boardListRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    public BoardListService(
            BoardListRepository boardListRepository,
            BoardRepository boardRepository,
            UserRepository userRepository
    ) {
        this.boardListRepository = boardListRepository;
        this.boardRepository = boardRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<BoardListResponse> getBoardLists(Long boardId) {
        User owner = getCurrentUser();
        getOwnedBoard(boardId, owner.getId());
        return boardListRepository.findAllByBoardIdOrderByPositionAsc(boardId).stream()
                .map(BoardListResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BoardListResponse getBoardList(Long listId) {
        User owner = getCurrentUser();
        return BoardListResponse.from(getOwnedBoardList(listId, owner.getId()));
    }

    @Transactional
    public BoardListResponse createBoardList(Long boardId, BoardListRequest request) {
        User owner = getCurrentUser();
        Board board = getOwnedBoardForUpdate(boardId, owner.getId());
        List<BoardList> existingLists =
                boardListRepository.findAllByBoardIdOrderByPositionAscForUpdate(boardId);
        normalizePositions(existingLists);

        BoardList boardList = new BoardList();
        boardList.setBoard(board);
        boardList.setTitle(request.title().trim());
        boardList.setPosition(existingLists.size());

        return BoardListResponse.from(boardListRepository.saveAndFlush(boardList));
    }

    @Transactional
    public BoardListResponse updateBoardList(Long listId, BoardListRequest request) {
        User owner = getCurrentUser();
        BoardList boardList = getOwnedBoardList(listId, owner.getId());
        boardList.setTitle(request.title().trim());
        return BoardListResponse.from(boardListRepository.saveAndFlush(boardList));
    }

    @Transactional
    public void deleteBoardList(Long listId) {
        User owner = getCurrentUser();
        Board board = boardRepository.findOwnedBoardByListIdForUpdate(listId, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("BoardList not found"));
        BoardList boardList = getOwnedBoardList(listId, owner.getId());
        List<BoardList> remainingLists =
                boardListRepository.findAllByBoardIdOrderByPositionAscForUpdate(board.getId())
                        .stream()
                        .filter(item -> !item.getId().equals(listId))
                        .toList();

        boardListRepository.delete(boardList);
        normalizePositions(remainingLists);
        boardListRepository.flush();
    }

    @Transactional
    public List<BoardListResponse> reorderBoardLists(
            Long boardId,
            BoardListReorderRequest request
    ) {
        User owner = getCurrentUser();
        getOwnedBoardForUpdate(boardId, owner.getId());

        List<BoardList> currentLists =
                boardListRepository.findAllByBoardIdOrderByPositionAscForUpdate(boardId);
        List<Long> orderedIds = request.orderedListIds();
        validateReorder(currentLists, orderedIds);

        Map<Long, BoardList> listsById = new HashMap<>();
        for (BoardList boardList : currentLists) {
            listsById.put(boardList.getId(), boardList);
        }

        for (int position = 0; position < orderedIds.size(); position++) {
            listsById.get(orderedIds.get(position)).setPosition(position);
        }

        boardListRepository.flush();
        return orderedIds.stream()
                .map(listsById::get)
                .map(BoardListResponse::from)
                .toList();
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

    private Board getOwnedBoardForUpdate(Long boardId, Long ownerId) {
        return boardRepository.findByIdAndOwnerIdForUpdate(boardId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found"));
    }

    private BoardList getOwnedBoardList(Long listId, Long ownerId) {
        return boardListRepository.findByIdAndBoardOwnerId(listId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("BoardList not found"));
    }

    private void normalizePositions(List<BoardList> boardLists) {
        for (int position = 0; position < boardLists.size(); position++) {
            boardLists.get(position).setPosition(position);
        }
    }

    private void validateReorder(List<BoardList> currentLists, List<Long> orderedIds) {
        if (orderedIds.size() != currentLists.size()) {
            throw new InvalidMoveException("Reorder request must contain every BoardList exactly once");
        }

        Set<Long> requestedIdSet = new HashSet<>(orderedIds);
        if (requestedIdSet.size() != orderedIds.size()) {
            throw new InvalidMoveException("Reorder request must not contain duplicate BoardList IDs");
        }

        Set<Long> currentIdSet = currentLists.stream()
                .map(BoardList::getId)
                .collect(Collectors.toSet());
        if (!requestedIdSet.equals(currentIdSet)) {
            throw new InvalidMoveException("Reorder request contains a missing or unrelated BoardList ID");
        }
    }
}
