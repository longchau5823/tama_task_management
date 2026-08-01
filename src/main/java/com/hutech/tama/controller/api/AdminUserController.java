package com.hutech.tama.controller.api;

import com.hutech.tama.dto.request.UpdateAdminRoleRequest;
import com.hutech.tama.dto.request.UpdateUserEnabledRequest;
import com.hutech.tama.dto.response.UserResponse;
import com.hutech.tama.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> getUsers() {
        return userService.getUsers();
    }

    @PatchMapping("/{userId}/enabled")
    public UserResponse updateEnabled( @PathVariable Long userId, @Valid @RequestBody UpdateUserEnabledRequest request, Authentication authentication) {
        return userService.updateEnabled(userId, request.enabled(), authentication.getName());
    }

    @PatchMapping("/{userId}/admin-role")
    public UserResponse updateAdminRole( @PathVariable Long userId, @Valid @RequestBody UpdateAdminRoleRequest request, Authentication authentication) {
        return userService.updateAdminRole(userId, request.admin(), authentication.getName());
    }
}
