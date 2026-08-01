package com.hutech.tama.service;

import com.hutech.tama.dto.response.BoardBackgroundUploadResponse;
import com.hutech.tama.entity.User;
import com.hutech.tama.exception.BusinessRuleException;
import com.hutech.tama.exception.ForbiddenOperationException;
import com.hutech.tama.exception.InvalidBoardBackgroundException;
import com.hutech.tama.exception.ResourceNotFoundException;
import com.hutech.tama.repository.BoardRepository;
import com.hutech.tama.repository.UserRepository;
import com.hutech.tama.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
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
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BoardBackgroundService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BoardBackgroundService.class);
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final int MAX_IMAGE_DIMENSION = 8000;
    private static final String CUSTOM_URL_PREFIX = "/api/board-backgrounds/files/";
    private static final Pattern CUSTOM_PATH_PATTERN = Pattern.compile(
            "^/api/board-backgrounds/files/(\\d+)/([a-f0-9\\-]{36}\\.(?:jpg|png))$"
    );
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile(
            "^[a-f0-9\\-]{36}\\.(?:jpg|png)$"
    );
    private static final Set<String> LIBRARY_BACKGROUNDS = Set.of(
            "/images/board-backgrounds/aurora.svg",
            "/images/board-backgrounds/coast.svg",
            "/images/board-backgrounds/mountains.svg",
            "/images/board-backgrounds/sunset.svg",
            "/images/board-backgrounds/forest.svg",
            "/images/board-backgrounds/night.svg"
    );

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final Path storageRoot;

    public BoardBackgroundService(
            BoardRepository boardRepository,
            UserRepository userRepository,
            @Value("${tama.storage.board-backgrounds:uploads/board-backgrounds}")
            String storageDirectory
    ) {
        this.boardRepository = boardRepository;
        this.userRepository = userRepository;
        this.storageRoot = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    public BoardBackgroundUploadResponse upload(MultipartFile file) {
        User owner = getCurrentUser();
        ImageFormat imageFormat = inspectImage(file);
        String fileName = UUID.randomUUID() + "." + imageFormat.extension();
        Path ownerDirectory = resolveOwnerDirectory(owner.getId());
        Path target = resolveOwnedFile(owner.getId(), fileName);

        try {
            Files.createDirectories(ownerDirectory);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể lưu ảnh nền Board", exception);
        }

        return new BoardBackgroundUploadResponse(customPublicPath(owner.getId(), fileName));
    }

    public StoredBoardBackground load(Long ownerId, String fileName) {
        User currentUser = getCurrentUser();
        if (!currentUser.getId().equals(ownerId)) {
            throw new ResourceNotFoundException("Không tìm thấy ảnh nền");
        }
        validateFileName(fileName);
        Path file = resolveOwnedFile(ownerId, fileName);
        if (!Files.isRegularFile(file)) {
            throw new ResourceNotFoundException("Không tìm thấy ảnh nền");
        }

        MediaType mediaType = fileName.endsWith(".png")
                ? MediaType.IMAGE_PNG
                : MediaType.IMAGE_JPEG;
        return new StoredBoardBackground(new FileSystemResource(file), mediaType);
    }

    public void deleteUnused(String fileName) {
        User owner = getCurrentUser();
        validateFileName(fileName);
        String publicPath = customPublicPath(owner.getId(), fileName);
        if (boardRepository.existsByOwnerIdAndBackgroundImage(owner.getId(), publicPath)) {
            throw new BusinessRuleException("Ảnh nền đang được một Board sử dụng");
        }
        deleteOwnedFile(owner.getId(), fileName);
    }

    public boolean isAllowedBackground(Long ownerId, String backgroundPath) {
        if (LIBRARY_BACKGROUNDS.contains(backgroundPath)) {
            return true;
        }
        CustomBackground customBackground = parseCustomPath(backgroundPath);
        return customBackground != null
                && customBackground.ownerId().equals(ownerId)
                && Files.isRegularFile(resolveOwnedFile(ownerId, customBackground.fileName()));
    }

    public void deleteAfterCommit(Long ownerId, String backgroundPath) {
        CustomBackground customBackground = parseCustomPath(backgroundPath);
        if (customBackground == null || !customBackground.ownerId().equals(ownerId)) {
            return;
        }

        Runnable deleteAction = () -> deleteOwnedFile(ownerId, customBackground.fileName());
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

    private ImageFormat inspectImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidBoardBackgroundException("Vui lòng chọn một file ảnh");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidBoardBackgroundException("Ảnh nền không được vượt quá 5 MB");
        }

        try (InputStream inputStream = file.getInputStream();
             ImageInputStream imageInput = ImageIO.createImageInputStream(inputStream)) {
            if (imageInput == null) {
                throw new InvalidBoardBackgroundException("File đã chọn không phải ảnh hợp lệ");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new InvalidBoardBackgroundException("File đã chọn không phải ảnh hợp lệ");
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
                    throw new InvalidBoardBackgroundException(
                            "Kích thước ảnh nền không hợp lệ hoặc vượt quá 8000 px"
                    );
                }
                return switch (formatName) {
                    case "jpeg", "jpg" -> new ImageFormat("jpg");
                    case "png" -> new ImageFormat("png");
                    default -> throw new InvalidBoardBackgroundException(
                            "Ảnh nền chỉ hỗ trợ định dạng JPEG hoặc PNG"
                    );
                };
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new InvalidBoardBackgroundException("Không thể đọc file ảnh đã chọn");
        }
    }

    private User getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new ForbiddenOperationException("Authentication is required"));
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authenticated user no longer exists"
                ));
    }

    private Path resolveOwnerDirectory(Long ownerId) {
        Path ownerDirectory = storageRoot.resolve(String.valueOf(ownerId)).normalize();
        if (!ownerDirectory.startsWith(storageRoot)) {
            throw new InvalidBoardBackgroundException("Đường dẫn lưu ảnh không hợp lệ");
        }
        return ownerDirectory;
    }

    private Path resolveOwnedFile(Long ownerId, String fileName) {
        Path ownerDirectory = resolveOwnerDirectory(ownerId);
        Path file = ownerDirectory.resolve(fileName).normalize();
        if (!file.startsWith(ownerDirectory)) {
            throw new InvalidBoardBackgroundException("Đường dẫn ảnh không hợp lệ");
        }
        return file;
    }

    private void validateFileName(String fileName) {
        if (fileName == null || !FILE_NAME_PATTERN.matcher(fileName).matches()) {
            throw new ResourceNotFoundException("Không tìm thấy ảnh nền");
        }
    }

    private CustomBackground parseCustomPath(String backgroundPath) {
        if (backgroundPath == null || !backgroundPath.startsWith(CUSTOM_URL_PREFIX)) {
            return null;
        }
        Matcher matcher = CUSTOM_PATH_PATTERN.matcher(backgroundPath);
        if (!matcher.matches()) {
            return null;
        }
        return new CustomBackground(Long.valueOf(matcher.group(1)), matcher.group(2));
    }

    private String customPublicPath(Long ownerId, String fileName) {
        return CUSTOM_URL_PREFIX + ownerId + "/" + fileName;
    }

    private void deleteOwnedFile(Long ownerId, String fileName) {
        try {
            Files.deleteIfExists(resolveOwnedFile(ownerId, fileName));
        } catch (IOException exception) {
            LOGGER.warn("Không thể xóa file ảnh nền {} của User {}", fileName, ownerId, exception);
        }
    }

    public record StoredBoardBackground(Resource resource, MediaType mediaType) {
    }

    private record CustomBackground(Long ownerId, String fileName) {
    }

    private record ImageFormat(String extension) {
    }
}
