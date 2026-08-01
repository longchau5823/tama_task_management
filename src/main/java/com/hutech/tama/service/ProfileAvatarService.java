package com.hutech.tama.service;

import com.hutech.tama.dto.response.UserResponse;
import com.hutech.tama.entity.User;
import com.hutech.tama.exception.ForbiddenOperationException;
import com.hutech.tama.exception.InvalidAvatarException;
import com.hutech.tama.exception.ResourceNotFoundException;
import com.hutech.tama.repository.UserRepository;
import com.hutech.tama.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProfileAvatarService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileAvatarService.class);
    private static final long MAX_FILE_SIZE = 2L * 1024 * 1024;
    private static final int MAX_IMAGE_DIMENSION = 4096;
    private static final String CUSTOM_URL_PREFIX = "/api/profile/avatar/files/";
    private static final Pattern CUSTOM_PATH_PATTERN = Pattern.compile(
            "^/api/profile/avatar/files/([a-f0-9\\-]{36}\\.(?:jpg|png))$"
    );
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile(
            "^[a-f0-9\\-]{36}\\.(?:jpg|png)$"
    );

    private final UserRepository userRepository;
    private final Path storageRoot;

    public ProfileAvatarService(
            UserRepository userRepository,
            @Value("${tama.storage.avatars:uploads/avatars}") String storageDirectory
    ) {
        this.userRepository = userRepository;
        this.storageRoot = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    @Transactional
    public UserResponse updateAvatar(MultipartFile file) {
        User currentUser = getCurrentUser();
        ImageFormat imageFormat = inspectImage(file);
        String fileName = UUID.randomUUID() + "." + imageFormat.extension();
        Path target = resolveOwnedFile(currentUser.getId(), fileName);

        storeFile(file, currentUser.getId(), target);
        registerRollbackCleanup(currentUser.getId(), fileName);

        String previousAvatarPath = currentUser.getAvatarPath();
        currentUser.setAvatarPath(customPublicPath(fileName));
        try {
            User savedUser = userRepository.saveAndFlush(currentUser);
            deletePreviousAfterCommit(currentUser.getId(), previousAvatarPath);
            return UserResponse.from(savedUser);
        } catch (RuntimeException exception) {
            deleteOwnedFile(currentUser.getId(), fileName);
            throw exception;
        }
    }

    @Transactional
    public UserResponse resetAvatar() {
        User currentUser = getCurrentUser();
        String previousAvatarPath = currentUser.getAvatarPath();
        if (User.DEFAULT_AVATAR_PATH.equals(previousAvatarPath)) {
            return UserResponse.from(currentUser);
        }

        currentUser.setAvatarPath(User.DEFAULT_AVATAR_PATH);
        User savedUser = userRepository.saveAndFlush(currentUser);
        deletePreviousAfterCommit(currentUser.getId(), previousAvatarPath);
        return UserResponse.from(savedUser);
    }

    @Transactional(readOnly = true)
    public StoredAvatar load(String fileName) {
        User currentUser = getCurrentUser();
        validateFileName(fileName);
        Path file = resolveOwnedFile(currentUser.getId(), fileName);
        if (!Files.isRegularFile(file)) {
            throw new ResourceNotFoundException("Không tìm thấy ảnh đại diện");
        }

        MediaType mediaType = fileName.endsWith(".png")
                ? MediaType.IMAGE_PNG
                : MediaType.IMAGE_JPEG;
        return new StoredAvatar(new FileSystemResource(file), mediaType);
    }

    private void storeFile(MultipartFile file, Long userId, Path target) {
        try {
            Files.createDirectories(resolveOwnerDirectory(userId));
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            deleteOwnedFile(userId, target.getFileName().toString());
            throw new IllegalStateException("Không thể lưu ảnh đại diện", exception);
        }
    }

    private ImageFormat inspectImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidAvatarException("Vui lòng chọn một file ảnh đại diện");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidAvatarException("Ảnh đại diện không được vượt quá 2 MB");
        }

        try (InputStream inputStream = file.getInputStream();
             ImageInputStream imageInput = ImageIO.createImageInputStream(inputStream)) {
            if (imageInput == null) {
                throw new InvalidAvatarException("File đã chọn không phải ảnh hợp lệ");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new InvalidAvatarException("File đã chọn không phải ảnh hợp lệ");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                String formatName = reader.getFormatName().toLowerCase(Locale.ROOT);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1
                        || width > MAX_IMAGE_DIMENSION
                        || height > MAX_IMAGE_DIMENSION) {
                    throw new InvalidAvatarException(
                            "Kích thước ảnh đại diện không hợp lệ hoặc vượt quá 4096 px"
                    );
                }
                return switch (formatName) {
                    case "jpeg", "jpg" -> new ImageFormat("jpg");
                    case "png" -> new ImageFormat("png");
                    default -> throw new InvalidAvatarException(
                            "Ảnh đại diện chỉ hỗ trợ định dạng JPEG hoặc PNG"
                    );
                };
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new InvalidAvatarException("Không thể đọc file ảnh đã chọn");
        }
    }

    private User getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new ForbiddenOperationException(
                        "Authentication is required"
                ));
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authenticated user no longer exists"
                ));
    }

    private Path resolveOwnerDirectory(Long userId) {
        Path ownerDirectory = storageRoot.resolve(String.valueOf(userId)).normalize();
        if (!ownerDirectory.startsWith(storageRoot)) {
            throw new InvalidAvatarException("Đường dẫn lưu ảnh đại diện không hợp lệ");
        }
        return ownerDirectory;
    }

    private Path resolveOwnedFile(Long userId, String fileName) {
        Path ownerDirectory = resolveOwnerDirectory(userId);
        Path file = ownerDirectory.resolve(fileName).normalize();
        if (!file.startsWith(ownerDirectory)) {
            throw new InvalidAvatarException("Đường dẫn ảnh đại diện không hợp lệ");
        }
        return file;
    }

    private void validateFileName(String fileName) {
        if (fileName == null || !FILE_NAME_PATTERN.matcher(fileName).matches()) {
            throw new ResourceNotFoundException("Không tìm thấy ảnh đại diện");
        }
    }

    private void deletePreviousAfterCommit(Long userId, String avatarPath) {
        String previousFileName = parseCustomFileName(avatarPath);
        if (previousFileName == null) {
            return;
        }

        Runnable deleteAction = () -> deleteOwnedFile(userId, previousFileName);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            deleteAction.run();
                        }
                    }
            );
        } else {
            deleteAction.run();
        }
    }

    private void registerRollbackCleanup(Long userId, String fileName) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != TransactionSynchronization.STATUS_COMMITTED) {
                            deleteOwnedFile(userId, fileName);
                        }
                    }
                }
        );
    }

    private String parseCustomFileName(String avatarPath) {
        if (avatarPath == null || !avatarPath.startsWith(CUSTOM_URL_PREFIX)) {
            return null;
        }
        Matcher matcher = CUSTOM_PATH_PATTERN.matcher(avatarPath);
        return matcher.matches() ? matcher.group(1) : null;
    }

    private String customPublicPath(String fileName) {
        return CUSTOM_URL_PREFIX + fileName;
    }

    private void deleteOwnedFile(Long userId, String fileName) {
        try {
            Files.deleteIfExists(resolveOwnedFile(userId, fileName));
        } catch (IOException exception) {
            LOGGER.warn(
                    "Không thể xóa file ảnh đại diện {} của User {}",
                    fileName,
                    userId,
                    exception
            );
        }
    }

    public record StoredAvatar(Resource resource, MediaType mediaType) {
    }

    private record ImageFormat(String extension) {
    }
}
