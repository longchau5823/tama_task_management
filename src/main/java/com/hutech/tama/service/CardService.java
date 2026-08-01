package com.hutech.tama.service;

import com.hutech.tama.dto.request.CardCompletionRequest;
import com.hutech.tama.dto.request.CardCreateRequest;
import com.hutech.tama.dto.request.CardMoveRequest;
import com.hutech.tama.dto.request.CardUpdateRequest;
import com.hutech.tama.dto.response.CardResponse;
import com.hutech.tama.entity.BoardList;
import com.hutech.tama.entity.Card;
import com.hutech.tama.entity.User;
import com.hutech.tama.enums.CardPriority;
import com.hutech.tama.exception.ForbiddenOperationException;
import com.hutech.tama.exception.InvalidCardPositionException;
import com.hutech.tama.exception.InvalidMoveException;
import com.hutech.tama.exception.ResourceNotFoundException;
import com.hutech.tama.repository.BoardListRepository;
import com.hutech.tama.repository.CardRepository;
import com.hutech.tama.repository.UserRepository;
import com.hutech.tama.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class CardService {

    private final CardRepository cardRepository;
    private final BoardListRepository boardListRepository;
    private final UserRepository userRepository;

    public CardService(
            CardRepository cardRepository,
            BoardListRepository boardListRepository,
            UserRepository userRepository
    ) {
        this.cardRepository = cardRepository;
        this.boardListRepository = boardListRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CardResponse> getCards(Long listId) {
        User owner = getCurrentUser();
        getOwnedBoardList(listId, owner.getId());
        return cardRepository.findAllByBoardListIdOrderByPositionAscIdAsc(listId).stream()
                .map(CardResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CardResponse getCard(Long cardId) {
        User owner = getCurrentUser();
        return CardResponse.from(getOwnedCard(cardId, owner.getId()));
    }

    @Transactional
    public CardResponse createCard(Long listId, CardCreateRequest request) {
        User owner = getCurrentUser();
        BoardList boardList = lockOwnedBoardLists(List.of(listId), owner.getId()).get(listId);
        List<Card> existingCards = cardRepository.findAllByBoardListIdOrderByPositionAscIdAscForUpdate(listId);
        normalizePositions(existingCards);

        Card card = new Card();
        card.setBoardList(boardList);
        card.setTitle(request.title().trim());
        card.setDescription(trimToNull(request.description()));
        card.setPosition(existingCards.size());
        card.setDueDate(normalizeDueDate(request.dueDate()));
        card.setPriority(request.priority() == null ? CardPriority.MEDIUM : request.priority());
        card.setCompleted(false);
        card.setCompletedAt(null);

        return CardResponse.from(cardRepository.saveAndFlush(card));
    }

    @Transactional
    public CardResponse updateCard(Long cardId, CardUpdateRequest request) {
        User owner = getCurrentUser();
        Card card = getOwnedCard(cardId, owner.getId());
        card.setTitle(request.title().trim());
        card.setDescription(trimToNull(request.description()));
        card.setDueDate(normalizeDueDate(request.dueDate()));
        card.setPriority(request.priority());
        return CardResponse.from(cardRepository.saveAndFlush(card));
    }

    @Transactional
    public CardResponse updateCompletion(Long cardId, CardCompletionRequest request) {
        User owner = getCurrentUser();
        Card card = getOwnedCard(cardId, owner.getId());

        if (request.completed()) {
            if (!Boolean.TRUE.equals(card.getCompleted()) || card.getCompletedAt() == null) {
                card.setCompletedAt(LocalDateTime.now().withNano(0));
            }
            card.setCompleted(true);
        } else {
            card.setCompleted(false);
            card.setCompletedAt(null);
        }

        return CardResponse.from(cardRepository.saveAndFlush(card));
    }

    @Transactional
    public CardResponse moveCard(Long cardId, CardMoveRequest request) {
        User owner = getCurrentUser();
        Long sourceListId = getOwnedCardListId(cardId, owner.getId());
        List<Long> listIdsToLock = List.of(sourceListId, request.targetListId()).stream()
                .distinct()
                .sorted()
                .toList();
        Map<Long, BoardList> lockedLists = lockOwnedBoardLists(
                listIdsToLock,
                owner.getId()
        );
        Card card = getOwnedCard(cardId, owner.getId());
        BoardList sourceList = lockedLists.get(sourceListId);
        BoardList targetList = lockedLists.get(request.targetListId());

        if (!Objects.equals(sourceList.getBoard().getId(), targetList.getBoard().getId())) {
            throw new InvalidMoveException(
                    "Card chỉ được di chuyển giữa các List trong cùng một Board"
            );
        }

        List<Card> sourceCards = getOrderedCards(sourceList.getId());
        int sourceIndex = findCardIndex(sourceCards, cardId);
        int targetPosition = request.targetPosition();

        if (Objects.equals(sourceList.getId(), targetList.getId())) {
            validateTargetPosition(targetPosition, sourceCards.size() - 1);
            sourceCards.remove(sourceIndex);
            sourceCards.add(targetPosition, card);
            normalizePositions(sourceCards);
        } else {
            List<Card> targetCards = getOrderedCards(targetList.getId());
            validateTargetPosition(targetPosition, targetCards.size());

            sourceCards.remove(sourceIndex);
            normalizePositions(sourceCards);

            card.setBoardList(targetList);
            targetCards.add(targetPosition, card);
            normalizePositions(targetCards);
        }

        cardRepository.flush();
        return CardResponse.from(card);
    }

    @Transactional
    public void deleteCard(Long cardId) {
        User owner = getCurrentUser();
        Long listId = getOwnedCardListId(cardId, owner.getId());
        lockOwnedBoardLists(List.of(listId), owner.getId());
        Card card = getOwnedCard(cardId, owner.getId());
        List<Card> remainingCards =
                cardRepository.findAllByBoardListIdOrderByPositionAscIdAscForUpdate(listId)
                        .stream()
                        .filter(item -> !item.getId().equals(cardId))
                        .toList();

        cardRepository.delete(card);
        normalizePositions(remainingCards);
        cardRepository.flush();
    }

    private User getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername().orElseThrow(() -> new ForbiddenOperationException("Authentication is required"));
        return userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("Authenticated user no longer exists"));
    }

    private BoardList getOwnedBoardList(Long listId, Long ownerId) {
        return boardListRepository.findByIdAndBoardOwnerId(listId, ownerId).orElseThrow(() -> new ResourceNotFoundException("BoardList not found"));
    }

    private Card getOwnedCard(Long cardId, Long ownerId) {
        return cardRepository.findByIdAndBoardListBoardOwnerId(cardId, ownerId).orElseThrow(() -> new ResourceNotFoundException("Card not found"));
    }

    private Long getOwnedCardListId(Long cardId, Long ownerId) {
        return cardRepository.findOwnedBoardListId(cardId, ownerId).orElseThrow(() -> new ResourceNotFoundException("Card not found"));
    }

    private Map<Long, BoardList> lockOwnedBoardLists(List<Long> listIds, Long ownerId) {
        List<BoardList> lockedLists = boardListRepository.findAllOwnedByIdInForUpdate(listIds, ownerId);
        if (lockedLists.size() != listIds.size()) {
            throw new ResourceNotFoundException("BoardList not found");
        }

        Map<Long, BoardList> listsById = new HashMap<>();
        for (BoardList boardList : lockedLists) {
            listsById.put(boardList.getId(), boardList);
        }
        return listsById;
    }

    private List<Card> getOrderedCards(Long listId) {
        return new ArrayList<>( cardRepository.findAllByBoardListIdOrderByPositionAscIdAscForUpdate(listId) );
    }

    private int findCardIndex(List<Card> cards, Long cardId) {
        for (int index = 0; index < cards.size(); index++) {
            if (Objects.equals(cards.get(index).getId(), cardId)) {
                return index;
            }
        }
        throw new InvalidMoveException("Card is missing from its source BoardList");
    }

    private void validateTargetPosition(int targetPosition, int maximumPosition) {
        if (targetPosition < 0 || targetPosition > maximumPosition) {
            throw new InvalidCardPositionException(
                    "Target position must be between 0 and " + maximumPosition
            );
        }
    }

    private void normalizePositions(List<Card> cards) {
        for (int position = 0; position < cards.size(); position++) {
            cards.get(position).setPosition(position);
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private LocalDateTime normalizeDueDate(LocalDateTime dueDate) {
        return dueDate == null ? null : dueDate.withNano(0);
    }
}
