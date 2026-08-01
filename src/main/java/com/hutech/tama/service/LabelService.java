package com.hutech.tama.service;

import com.hutech.tama.dto.request.LabelCreateRequest;
import com.hutech.tama.dto.request.LabelUpdateRequest;
import com.hutech.tama.dto.response.LabelResponse;
import com.hutech.tama.entity.Card;
import com.hutech.tama.entity.Label;
import com.hutech.tama.entity.User;
import com.hutech.tama.exception.DuplicateResourceException;
import com.hutech.tama.exception.ForbiddenOperationException;
import com.hutech.tama.exception.ResourceNotFoundException;
import com.hutech.tama.repository.CardRepository;
import com.hutech.tama.repository.LabelRepository;
import com.hutech.tama.repository.UserRepository;
import com.hutech.tama.security.SecurityUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class LabelService {

    private final LabelRepository labelRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;

    public LabelService(
            LabelRepository labelRepository,
            CardRepository cardRepository,
            UserRepository userRepository
    ) {
        this.labelRepository = labelRepository;
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<LabelResponse> getLabels() {
        User owner = getCurrentUser();
        return labelRepository.findAllByOwnerIdOrderByNameAsc(owner.getId()).stream()
                .map(LabelResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public LabelResponse getLabel(Long labelId) {
        User owner = getCurrentUser();
        return LabelResponse.from(getOwnedLabel(labelId, owner.getId()));
    }

    @Transactional
    public LabelResponse createLabel(LabelCreateRequest request) {
        User owner = getCurrentUser();
        String name = request.name().trim();
        ensureUniqueName(owner.getId(), name, null);

        Label label = new Label();
        label.setOwner(owner);
        label.setName(name);
        label.setColor(normalizeColor(request.color()));
        return LabelResponse.from(saveLabel(label));
    }

    @Transactional
    public LabelResponse updateLabel(Long labelId, LabelUpdateRequest request) {
        User owner = getCurrentUser();
        Label label = getOwnedLabel(labelId, owner.getId());
        String name = request.name().trim();
        ensureUniqueName(owner.getId(), name, labelId);

        label.setName(name);
        label.setColor(normalizeColor(request.color()));
        return LabelResponse.from(saveLabel(label));
    }

    @Transactional
    public void deleteLabel(Long labelId) {
        User owner = getCurrentUser();
        Label label = getOwnedLabel(labelId, owner.getId());
        labelRepository.delete(label);
        labelRepository.flush();
    }

    @Transactional(readOnly = true)
    public List<LabelResponse> getCardLabels(Long cardId) {
        User owner = getCurrentUser();
        Card card = getOwnedCard(cardId, owner.getId());
        return card.getLabels().stream()
                .sorted(Comparator.comparing(Label::getName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Label::getId))
                .map(LabelResponse::from)
                .toList();
    }

    @Transactional
    public void attachLabel(Long cardId, Long labelId) {
        User owner = getCurrentUser();
        Card card = getOwnedCard(cardId, owner.getId());
        Label label = getOwnedLabel(labelId, owner.getId());
        if (card.getLabels().add(label)) {
            cardRepository.saveAndFlush(card);
        }
    }

    @Transactional
    public void detachLabel(Long cardId, Long labelId) {
        User owner = getCurrentUser();
        Card card = getOwnedCard(cardId, owner.getId());
        Label label = getOwnedLabel(labelId, owner.getId());
        if (card.getLabels().remove(label)) {
            cardRepository.saveAndFlush(card);
        }
    }

    private Label saveLabel(Label label) {
        try {
            return labelRepository.saveAndFlush(label);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateResourceException("A Label with this name already exists");
        }
    }

    private void ensureUniqueName(Long ownerId, String name, Long ignoredLabelId) {
        boolean exists = ignoredLabelId == null
                ? labelRepository.existsByOwnerIdAndNameIgnoreCase(ownerId, name)
                : labelRepository.existsByOwnerIdAndNameIgnoreCaseAndIdNot(
                        ownerId,
                        name,
                        ignoredLabelId
                );
        if (exists) {
            throw new DuplicateResourceException("A Label with this name already exists");
        }
    }

    private String normalizeColor(String color) {
        return color.trim().toUpperCase(Locale.ROOT);
    }

    private User getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new ForbiddenOperationException("Authentication is required"));
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authenticated user no longer exists"
                ));
    }

    private Label getOwnedLabel(Long labelId, Long ownerId) {
        return labelRepository.findByIdAndOwnerId(labelId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Label not found"));
    }

    private Card getOwnedCard(Long cardId, Long ownerId) {
        return cardRepository.findByIdAndBoardListBoardOwnerId(cardId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
    }
}
