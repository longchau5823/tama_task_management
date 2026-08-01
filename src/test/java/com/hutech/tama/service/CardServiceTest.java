package com.hutech.tama.service;

import com.hutech.tama.dto.request.CardCreateRequest;
import com.hutech.tama.dto.request.CardUpdateRequest;
import com.hutech.tama.dto.response.CardResponse;
import com.hutech.tama.entity.Board;
import com.hutech.tama.entity.BoardList;
import com.hutech.tama.entity.Card;
import com.hutech.tama.entity.User;
import com.hutech.tama.enums.CardPriority;
import com.hutech.tama.repository.BoardListRepository;
import com.hutech.tama.repository.CardRepository;
import com.hutech.tama.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private BoardListRepository boardListRepository;

    @Mock
    private UserRepository userRepository;

    private CardService cardService;
    private User owner;
    private BoardList boardList;

    @BeforeEach
    void setUp() {
        cardService = new CardService(cardRepository, boardListRepository, userRepository);
        owner = new User();
        owner.setId(1L);
        owner.setUsername("owner");

        Board board = new Board();
        board.setId(2L);
        board.setOwner(owner);

        boardList = new BoardList();
        boardList.setId(3L);
        boardList.setBoard(board);
        boardList.setTitle("Todo");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner", "n/a", List.of())
        );
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createCardAllowsMissingDueDate() {
        when(boardListRepository.findAllOwnedByIdInForUpdate(List.of(3L), 1L))
                .thenReturn(List.of(boardList));
        when(cardRepository.findAllByBoardListIdOrderByPositionAscIdAscForUpdate(3L))
                .thenReturn(List.of());
        when(cardRepository.saveAndFlush(any(Card.class))).thenAnswer(invocation -> {
            Card card = invocation.getArgument(0);
            card.setId(10L);
            return card;
        });

        CardResponse response = cardService.createCard(
                3L,
                new CardCreateRequest("Không có hạn", null, null, null)
        );

        assertThat(response.dueDate()).isNull();
        assertThat(response.priority()).isEqualTo(CardPriority.MEDIUM);
        assertThat(response.position()).isZero();
    }

    @Test
    void updateCardCanClearExistingDueDate() {
        Card card = new Card();
        card.setId(10L);
        card.setBoardList(boardList);
        card.setTitle("Có hạn");
        card.setPosition(0);
        card.setDueDate(LocalDateTime.of(2026, 7, 30, 18, 0));
        card.setCompleted(false);
        card.setPriority(CardPriority.MEDIUM);

        when(cardRepository.findByIdAndBoardListBoardOwnerId(10L, 1L))
                .thenReturn(Optional.of(card));
        when(cardRepository.saveAndFlush(card)).thenReturn(card);

        CardResponse response = cardService.updateCard(
                10L,
                new CardUpdateRequest("Đã bỏ hạn", null, null, CardPriority.HIGH)
        );

        assertThat(response.dueDate()).isNull();
        assertThat(response.priority()).isEqualTo(CardPriority.HIGH);
        assertThat(card.getDueDate()).isNull();
    }
}
