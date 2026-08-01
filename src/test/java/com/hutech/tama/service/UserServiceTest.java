package com.hutech.tama.service;

import com.hutech.tama.dto.response.UserResponse;
import com.hutech.tama.entity.Role;
import com.hutech.tama.entity.User;
import com.hutech.tama.enums.RoleName;
import com.hutech.tama.exception.BusinessRuleException;
import com.hutech.tama.repository.RoleRepository;
import com.hutech.tama.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Test
    void administratorCannotDisableOwnAccount() {
        User admin = user(1L, "admin", true, RoleName.USER, RoleName.ADMIN);
        UserService userService = new UserService(userRepository, roleRepository);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> userService.updateEnabled(1L, false, "admin"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("own account");

        verify(userRepository, never()).save(any());
    }

    @Test
    void lastEnabledAdministratorCannotBeDisabled() {
        User currentAdmin = user(1L, "current-admin", true, RoleName.USER, RoleName.ADMIN);
        User targetAdmin = user(2L, "target-admin", true, RoleName.USER, RoleName.ADMIN);
        UserService userService = new UserService(userRepository, roleRepository);
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetAdmin));
        when(userRepository.findByUsername("current-admin")).thenReturn(Optional.of(currentAdmin));
        when(userRepository.countEnabledUsersByRole(RoleName.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> userService.updateEnabled(2L, false, "current-admin"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("last enabled administrator");

        verify(userRepository, never()).save(any());
    }

    @Test
    void administratorCannotRemoveOwnAdminRole() {
        User admin = user(1L, "admin", true, RoleName.USER, RoleName.ADMIN);
        UserService userService = new UserService(userRepository, roleRepository);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> userService.updateAdminRole(1L, false, "admin"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("own ADMIN role");

        verify(userRepository, never()).save(any());
    }

    @Test
    void lastEnabledAdministratorCannotLoseAdminRole() {
        User currentAdmin = user(1L, "current-admin", true, RoleName.USER, RoleName.ADMIN);
        User targetAdmin = user(2L, "target-admin", true, RoleName.USER, RoleName.ADMIN);
        UserService userService = new UserService(userRepository, roleRepository);
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetAdmin));
        when(userRepository.findByUsername("current-admin")).thenReturn(Optional.of(currentAdmin));
        when(userRepository.countEnabledUsersByRole(RoleName.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> userService.updateAdminRole(2L, false, "current-admin"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("last enabled administrator");

        verify(userRepository, never()).save(any());
    }

    @Test
    void grantingAdminKeepsUserRole() {
        User currentAdmin = user(1L, "admin", true, RoleName.USER, RoleName.ADMIN);
        User target = user(2L, "member", true, RoleName.USER);
        Role adminRole = role(RoleName.ADMIN);
        UserService userService = new UserService(userRepository, roleRepository);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(currentAdmin));
        when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.of(adminRole));
        when(userRepository.save(target)).thenReturn(target);

        UserResponse response = userService.updateAdminRole(2L, true, "admin");

        assertThat(response.roles()).containsExactlyInAnyOrder(RoleName.USER, RoleName.ADMIN);
    }

    private User user(Long id, String username, boolean enabled, RoleName... roles) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("$2a$10$test");
        user.setEnabled(enabled);
        for (RoleName roleName : roles) {
            user.getRoles().add(role(roleName));
        }
        return user;
    }

    private Role role(RoleName name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }
}
