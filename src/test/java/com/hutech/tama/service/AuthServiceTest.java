package com.hutech.tama.service;

import com.hutech.tama.dto.request.RegisterRequest;
import com.hutech.tama.dto.response.UserResponse;
import com.hutech.tama.entity.Role;
import com.hutech.tama.entity.User;
import com.hutech.tama.enums.RoleName;
import com.hutech.tama.exception.DuplicateResourceException;
import com.hutech.tama.repository.RoleRepository;
import com.hutech.tama.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void registerSavesEnabledUserWithBcryptPasswordAndUserRole() {
        Role userRole = role(RoleName.USER);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(userRole));
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            user.setCreatedAt(LocalDateTime.of(2026, 7, 23, 20, 0));
            return user;
        });

        UserResponse response = authService.register(
                new RegisterRequest("  newuser  ", "  USER@Example.com ", "password123")
        );

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getUsername()).isEqualTo("newuser");
        assertThat(saved.getEmail()).isEqualTo("user@example.com");
        assertThat(saved.getAvatarPath()).isEqualTo(User.DEFAULT_AVATAR_PATH);
        assertThat(saved.getEnabled()).isTrue();
        assertThat(saved.getRoles()).containsExactly(userRole);
        assertThat(saved.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", saved.getPassword())).isTrue();
        assertThat(response.roles()).containsExactly(RoleName.USER);
        assertThat(response.enabled()).isTrue();
    }

    @Test
    void registerRejectsDuplicateUsername() {
        when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("existing", "new@example.com", "password123")
        )).isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("newuser", "existing@example.com", "password123")
        )).isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void userResponseDoesNotDefinePasswordField() {
        assertThat(Arrays.stream(UserResponse.class.getRecordComponents())
                .map(component -> component.getName()))
                .doesNotContain("password", "passwordHash");
    }

    private Role role(RoleName name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }
}
