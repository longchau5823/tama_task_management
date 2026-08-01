package com.hutech.tama.service;

import com.hutech.tama.dto.request.PasswordChangeRequest;
import com.hutech.tama.dto.request.ProfileUpdateRequest;
import com.hutech.tama.dto.response.UserResponse;
import com.hutech.tama.entity.User;
import com.hutech.tama.exception.BusinessRuleException;
import com.hutech.tama.exception.DuplicateResourceException;
import com.hutech.tama.exception.ForbiddenOperationException;
import com.hutech.tama.exception.InvalidPasswordException;
import com.hutech.tama.exception.ResourceNotFoundException;
import com.hutech.tama.repository.UserRepository;
import com.hutech.tama.security.SecurityUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse updateEmail(ProfileUpdateRequest request) {
        User currentUser = getCurrentUser();
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        if (currentUser.getEmail().equals(normalizedEmail)) {
            return UserResponse.from(currentUser);
        }
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, currentUser.getId())) {
            throw new DuplicateResourceException("Email is already in use");
        }

        currentUser.setEmail(normalizedEmail);
        try {
            return UserResponse.from(userRepository.saveAndFlush(currentUser));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateResourceException("Email is already in use");
        }
    }

    @Transactional
    public void changePassword(PasswordChangeRequest request) {
        User currentUser = getCurrentUser();

        if (!passwordEncoder.matches(request.currentPassword(), currentUser.getPassword())) {
            throw new InvalidPasswordException("Current password is incorrect");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new InvalidPasswordException("Password confirmation does not match the new password");
        }
        if (passwordEncoder.matches(request.newPassword(), currentUser.getPassword())) {
            throw new BusinessRuleException("New password must be different from the current password");
        }

        currentUser.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.saveAndFlush(currentUser);
    }

    private User getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new ForbiddenOperationException("Authentication is required"));
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user no longer exists"));
    }
}
