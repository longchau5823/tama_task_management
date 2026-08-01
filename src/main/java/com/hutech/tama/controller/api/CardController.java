package com.hutech.tama.controller.api;

import com.hutech.tama.dto.request.CardCompletionRequest;
import com.hutech.tama.dto.request.CardCreateRequest;
import com.hutech.tama.dto.request.CardMoveRequest;
import com.hutech.tama.dto.request.CardUpdateRequest;
import com.hutech.tama.dto.response.CardResponse;
import com.hutech.tama.dto.response.LabelResponse;
import com.hutech.tama.service.CardService;
import com.hutech.tama.service.LabelService;
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
public class CardController {

    private final CardService cardService;
    private final LabelService labelService;

    public CardController(CardService cardService, LabelService labelService) {
        this.cardService = cardService;
        this.labelService = labelService;
    }

    @GetMapping("/lists/{listId}/cards")
    public List<CardResponse> getCards(@PathVariable Long listId) {
        return cardService.getCards(listId);
    }

    @GetMapping("/cards/{cardId}")
    public CardResponse getCard(@PathVariable Long cardId) {
        return cardService.getCard(cardId);
    }

    @PostMapping("/lists/{listId}/cards")
    @ResponseStatus(HttpStatus.CREATED)
    public CardResponse createCard( @PathVariable Long listId, @Valid @RequestBody CardCreateRequest request ) {
        return cardService.createCard(listId, request);
    }

    @PutMapping("/cards/{cardId}")
    public CardResponse updateCard( @PathVariable Long cardId, @Valid @RequestBody CardUpdateRequest request ) {
        return cardService.updateCard(cardId, request);
    }

    @PatchMapping("/cards/{cardId}/completion")
    public CardResponse updateCompletion( @PathVariable Long cardId, @Valid @RequestBody CardCompletionRequest request ) {
        return cardService.updateCompletion(cardId, request);
    }

    @PatchMapping("/cards/{cardId}/move")
    public CardResponse moveCard( @PathVariable Long cardId, @Valid @RequestBody CardMoveRequest request ) {
        return cardService.moveCard(cardId, request);
    }

    @DeleteMapping("/cards/{cardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCard(@PathVariable Long cardId) {
        cardService.deleteCard(cardId);
    }

    @GetMapping("/cards/{cardId}/labels")
    public List<LabelResponse> getCardLabels(@PathVariable Long cardId) {
        return labelService.getCardLabels(cardId);
    }

    @PostMapping("/cards/{cardId}/labels/{labelId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void attachLabel(@PathVariable Long cardId, @PathVariable Long labelId) {
        labelService.attachLabel(cardId, labelId);
    }

    @DeleteMapping("/cards/{cardId}/labels/{labelId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void detachLabel(@PathVariable Long cardId, @PathVariable Long labelId) {
        labelService.detachLabel(cardId, labelId);
    }
}
