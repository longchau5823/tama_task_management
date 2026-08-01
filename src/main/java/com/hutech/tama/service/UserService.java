package com.hutech.tama.service;

import com.hutech.tama.dto.response.UserResponse;
import com.hutech.tama.entity.Role;
import com.hutech.tama.entity.User;
import com.hutech.tama.enums.RoleName;
import com.hutech.tama.exception.BusinessRuleException;
import com.hutech.tama.exception.ResourceNotFoundException;
import com.hutech.tama.repository.RoleRepository;
import com.hutech.tama.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserResponse updateEnabled(Long userId, Boolean enabled, String currentUsername) {
        if (enabled == null) {
            throw new BusinessRuleException("Enabled status is required");
        }

        User target = getUser(userId);
        User currentUser = getCurrentUser(currentUsername);

        if (Boolean.valueOf(enabled).equals(target.getEnabled())) {
            return UserResponse.from(target);
        }
        if (!enabled && target.getId().equals(currentUser.getId())) {
            throw new BusinessRuleException("Administrators cannot disable their own account");
        }
        if (!enabled && isAdmin(target) && isLastEnabledAdmin()) {
            throw new BusinessRuleException("The last enabled administrator cannot be disabled");
        }

        target.setEnabled(enabled);
        return UserResponse.from(userRepository.save(target));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserResponse updateAdminRole(Long userId, Boolean admin, String currentUsername) {
        if (admin == null) {
            throw new BusinessRuleException("Admin role status is required");
        }

        User target = getUser(userId);
        User currentUser = getCurrentUser(currentUsername);
        boolean currentlyAdmin = isAdmin(target);

        if (admin == currentlyAdmin) {
            return UserResponse.from(target);
        }
        if (!admin && target.getId().equals(currentUser.getId())) {
            throw new BusinessRuleException("Administrators cannot remove their own ADMIN role");
        }
        if (!admin && Boolean.TRUE.equals(target.getEnabled()) && isLastEnabledAdmin()) {
            throw new BusinessRuleException("The last enabled administrator cannot lose the ADMIN role");
        }

        if (admin) {
            Role adminRole = getRole(RoleName.ADMIN);
            target.getRoles().add(adminRole);
        } else {
            target.getRoles().removeIf(role -> role.getName() == RoleName.ADMIN);
        }

        return UserResponse.from(userRepository.save(target));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user no longer exists"));
    }

    private Role getRole(RoleName roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException(roleName + " role is missing"));
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ADMIN);
    }

    private boolean isLastEnabledAdmin() {
        return userRepository.countEnabledUsersByRole(RoleName.ADMIN) <= 1;
    }
}
