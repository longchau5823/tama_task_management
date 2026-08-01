package com.hutech.tama.security;

import com.hutech.tama.dto.response.UserResponse;
import com.hutech.tama.enums.RoleName;
import com.hutech.tama.service.AuthService;
import com.hutech.tama.service.ProfileAvatarService;
import com.hutech.tama.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ProfileAvatarService profileAvatarService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    private UserResponse aliceResponse;

    @BeforeEach
    void setUp() {
        aliceResponse = new UserResponse(
                1L,
                "alice",
                "alice@example.com",
                true,
                Set.of(RoleName.USER),
                LocalDateTime.of(2026, 7, 23, 20, 0)
        );

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        UserDetails activeUser = User.withUsername("alice")
                .password(encoder.encode("password123"))
                .roles("USER")
                .build();
        UserDetails disabledUser = User.withUsername("disabled")
                .password(encoder.encode("password123"))
                .roles("USER")
                .disabled(true)
                .build();
        UserDetails activeAdmin = User.withUsername("admin")
                .password(encoder.encode("password123"))
                .roles("USER", "ADMIN")
                .build();
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(activeUser);
        when(userDetailsService.loadUserByUsername("disabled")).thenReturn(disabledUser);
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(activeAdmin);
    }

    @Test
    void publicPagesAreAccessible() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
        mockMvc.perform(get("/register")).andExpect(status().isOk());
    }

    @Test
    void authenticatedUserCanRenderBoardPageWithCardDialogAreas() throws Exception {
        mockMvc.perform(get("/boards/1").with(user("alice").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"card-dialog-main\"")))
                .andExpect(content().string(
                        containsString("class=\"card-dialog-main-scroll\"")
                ))
                .andExpect(content().string(
                        containsString("class=\"card-dialog-sidebar-content\"")
                ))
                .andExpect(content().string(
                        containsString(
                                "class=\"dialog-actions card-dialog-sidebar-actions\""
                        )
                ))
                .andExpect(content().string(containsString("rows=\"1\"")))
                .andExpect(content().string(not(containsString("trong List"))))
                .andExpect(content().string(not(containsString(
                        "Ghi chú hoặc cập nhật tiến độ cho thẻ công việc này."
                ))))
                .andExpect(content().string(not(containsString(
                        "Chưa có bình luận nào."
                ))));
    }

    @Test
    void authenticatedUserCanRenderProfileAvatarEditor() throws Exception {
        mockMvc.perform(get("/profile").with(user("alice").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "id=\"profile-avatar-preview\""
                )))
                .andExpect(content().string(containsString(
                        "id=\"profile-avatar-file\""
                )))
                .andExpect(content().string(containsString(
                        "id=\"profile-avatar-submit\""
                )))
                .andExpect(content().string(containsString(
                        "id=\"profile-avatar-reset\""
                )));
    }

    @Test
    void authenticatedUserCanUploadAndResetAvatarWithCsrf() throws Exception {
        UserResponse customAvatarResponse = new UserResponse(
                1L,
                "alice",
                "alice@example.com",
                true,
                Set.of(RoleName.USER),
                aliceResponse.createdAt(),
                LocalDateTime.of(2026, 7, 30, 15, 0),
                "/api/profile/avatar/files/00000000-0000-0000-0000-000000000001.png"
        );
        when(profileAvatarService.updateAvatar(any())).thenReturn(customAvatarResponse);
        when(profileAvatarService.resetAvatar()).thenReturn(aliceResponse);
        MockMultipartFile avatar = new MockMultipartFile(
                "file",
                "avatar.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/profile/avatar")
                        .file(avatar)
                        .with(user("alice").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarPath").value(
                        customAvatarResponse.avatarPath()
                ));

        mockMvc.perform(delete("/api/profile/avatar")
                        .with(user("alice").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarPath").value(
                        "/images/default-avatar.svg"
                ));
    }

    @Test
    void avatarUploadWithoutCsrfIsForbidden() throws Exception {
        MockMultipartFile avatar = new MockMultipartFile(
                "file",
                "avatar.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/profile/avatar")
                        .file(avatar)
                        .with(user("alice").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void validRegistrationReturnsCreatedWithoutPassword() throws Exception {
        when(authService.register(any())).thenReturn(aliceResponse);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "email": "alice@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void registrationWithoutCsrfIsForbiddenWithJsonError() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "email": "alice@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void correctLoginCreatesAuthenticatedSession() throws Exception {
        mockMvc.perform(formLogin("/login").user("alice").password("password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(authenticated().withUsername("alice"));
    }

    @Test
    void wrongPasswordFailsLogin() throws Exception {
        mockMvc.perform(formLogin("/login").user("alice").password("wrong-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void disabledUserCannotLogin() throws Exception {
        mockMvc.perform(formLogin("/login").user("disabled").password("password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void logoutUsesPostAndRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/logout")
                        .with(user("alice").roles("USER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"))
                .andExpect(unauthenticated());
    }

    @Test
    void anonymousUserCannotOpenDashboardOrCurrentUserApi() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void regularUserCannotAccessAdminPageOrApi() throws Exception {
        mockMvc.perform(get("/admin/users").with(user("alice").roles("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/users").with(user("alice").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void administratorCanAccessAdminPageAndApi() throws Exception {
        when(userService.getUsers()).thenReturn(List.of(aliceResponse));

        mockMvc.perform(get("/admin/users").with(user("admin").roles("USER", "ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/users").with(user("admin").roles("USER", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alice"));
    }

    @Test
    void administratorCanCallUserManagementPatchApisWithCsrf() throws Exception {
        UserResponse disabledAlice = new UserResponse(
                1L,
                "alice",
                "alice@example.com",
                false,
                Set.of(RoleName.USER),
                aliceResponse.createdAt()
        );
        UserResponse promotedAlice = new UserResponse(
                1L,
                "alice",
                "alice@example.com",
                true,
                Set.of(RoleName.USER, RoleName.ADMIN),
                aliceResponse.createdAt()
        );
        when(userService.updateEnabled(1L, false, "admin")).thenReturn(disabledAlice);
        when(userService.updateAdminRole(1L, true, "admin")).thenReturn(promotedAlice);

        mockMvc.perform(patch("/api/admin/users/1/enabled")
                        .with(user("admin").roles("USER", "ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(patch("/api/admin/users/1/admin-role")
                        .with(user("admin").roles("USER", "ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"admin\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").isArray());
    }

    @Test
    void currentUserApiReturnsAuthenticatedUser() throws Exception {
        when(authService.getCurrentUser("alice")).thenReturn(aliceResponse);

        mockMvc.perform(get("/api/auth/me").with(user("alice").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void disabledAccountLosesAnExistingAuthenticatedSession() throws Exception {
        UserDetails disabledAlice = User.withUsername("alice")
                .password("n/a")
                .roles("USER")
                .disabled(true)
                .build();
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(disabledAlice);

        mockMvc.perform(get("/api/auth/me").with(user("alice").roles("USER")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void changedAuthoritiesInvalidateAnExistingAdminSession() throws Exception {
        UserDetails demotedAdmin = User.withUsername("admin")
                .password("n/a")
                .roles("USER")
                .build();
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(demotedAdmin);

        mockMvc.perform(get("/api/admin/users").with(user("admin").roles("USER", "ADMIN")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
