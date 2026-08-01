package com.hutech.tama.service;

import com.hutech.tama.dto.response.UserResponse;
import com.hutech.tama.entity.User;
import com.hutech.tama.exception.InvalidAvatarException;
import com.hutech.tama.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileAvatarServiceTest {

    @Mock
    private UserRepository userRepository;

    @TempDir
    private Path storageRoot;

    private ProfileAvatarService profileAvatarService;
    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("alice");
        currentUser.setEmail("alice@example.com");
        currentUser.setPassword("$2a$10$test");
        currentUser.setEnabled(true);

        profileAvatarService = new ProfileAvatarService(
                userRepository,
                storageRoot.toString()
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "alice",
                        "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(currentUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validPngUpdatesAvatarAndCanBeLoaded() throws Exception {
        when(userRepository.saveAndFlush(currentUser)).thenReturn(currentUser);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                MediaType.IMAGE_PNG_VALUE,
                pngImage()
        );

        UserResponse response = profileAvatarService.updateAvatar(file);

        assertThat(response.avatarPath())
                .matches("^/api/profile/avatar/files/[a-f0-9\\-]{36}\\.png$");
        String fileName = response.avatarPath()
                .substring(response.avatarPath().lastIndexOf('/') + 1);
        assertThat(Files.isRegularFile(storageRoot.resolve("1").resolve(fileName))).isTrue();
        assertThat(profileAvatarService.load(fileName).mediaType())
                .isEqualTo(MediaType.IMAGE_PNG);
        verify(userRepository).saveAndFlush(currentUser);
    }

    @Test
    void nonImageFileIsRejectedWithoutUpdatingUser() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                MediaType.IMAGE_PNG_VALUE,
                "not-an-image".getBytes()
        );

        assertThatThrownBy(() -> profileAvatarService.updateAvatar(file))
                .isInstanceOf(InvalidAvatarException.class)
                .hasMessageContaining("không phải ảnh hợp lệ");

        verify(userRepository, never()).saveAndFlush(currentUser);
    }

    @Test
    void avatarLargerThanTwoMegabytesIsRejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[2 * 1024 * 1024 + 1]
        );

        assertThatThrownBy(() -> profileAvatarService.updateAvatar(file))
                .isInstanceOf(InvalidAvatarException.class)
                .hasMessageContaining("2 MB");

        verify(userRepository, never()).saveAndFlush(currentUser);
    }

    @Test
    void resetRestoresDefaultAndDeletesPreviousCustomFile() throws Exception {
        String fileName = UUID.randomUUID() + ".jpg";
        Path customFile = storageRoot.resolve("1").resolve(fileName);
        Files.createDirectories(customFile.getParent());
        Files.write(customFile, new byte[]{1, 2, 3});
        currentUser.setAvatarPath("/api/profile/avatar/files/" + fileName);
        when(userRepository.saveAndFlush(currentUser)).thenReturn(currentUser);

        UserResponse response = profileAvatarService.resetAvatar();

        assertThat(response.avatarPath()).isEqualTo(User.DEFAULT_AVATAR_PATH);
        assertThat(Files.exists(customFile)).isFalse();
        verify(userRepository).saveAndFlush(currentUser);
    }

    private byte[] pngImage() throws Exception {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(79, 70, 229));
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
