-- ============================================================
-- TaMa - Database Initialization Script
-- Database: tama_db
-- Target: MySQL 8.4, UTF-8 (utf8mb4)
-- ============================================================

CREATE DATABASE IF NOT EXISTS tama_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE tama_db;

SET NAMES utf8mb4;

-- Cho phép chạy lại script từ đầu.
-- LƯU Ý: Phần này sẽ xóa toàn bộ dữ liệu trong các bảng TaMa hiện có.
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS comments;
DROP TABLE IF EXISTS card_labels;
DROP TABLE IF EXISTS labels;
DROP TABLE IF EXISTS cards;
DROP TABLE IF EXISTS board_lists;
DROP TABLE IF EXISTS boards;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 1. USERS
-- Lưu tài khoản đăng nhập. Mật khẩu phải được mã hóa bằng BCrypt
-- tại tầng ứng dụng trước khi lưu.
-- avatar_path lưu ảnh mặc định hoặc đường dẫn API của avatar tùy chỉnh;
-- file ảnh nằm ngoài database tại thư mục upload cấu hình của ứng dụng.
-- ============================================================
CREATE TABLE users (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)     NOT NULL,
    email       VARCHAR(100)    NOT NULL,
    avatar_path VARCHAR(500)    NOT NULL DEFAULT '/images/default-avatar.svg',
    password    VARCHAR(255)    NOT NULL,
    enabled     BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                         ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_username_not_blank
        CHECK (CHAR_LENGTH(TRIM(username)) > 0),
    CONSTRAINT chk_users_email_not_blank
        CHECK (CHAR_LENGTH(TRIM(email)) > 0),
    CONSTRAINT chk_users_avatar_path_not_blank
        CHECK (CHAR_LENGTH(TRIM(avatar_path)) > 0)
) ENGINE = InnoDB;

-- ============================================================
-- 2. ROLES
-- Vai trò hệ thống: ADMIN và USER.
-- ============================================================
CREATE TABLE roles (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name         VARCHAR(30)     NOT NULL,
    description  VARCHAR(255)    NULL,

    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uq_roles_name UNIQUE (name),
    CONSTRAINT chk_roles_name_not_blank
        CHECK (CHAR_LENGTH(TRIM(name)) > 0)
) ENGINE = InnoDB;

-- ============================================================
-- 3. USER_ROLES
-- Quan hệ nhiều-nhiều giữa users và roles.
-- ============================================================
CREATE TABLE user_roles (
    user_id  BIGINT UNSIGNED NOT NULL,
    role_id  BIGINT UNSIGNED NOT NULL,

    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),

    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id)
        REFERENCES roles (id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
) ENGINE = InnoDB;

CREATE INDEX idx_user_roles_role_id
    ON user_roles (role_id);

-- ============================================================
-- 4. BOARDS
-- Mỗi Board thuộc sở hữu của đúng một User.
-- ============================================================
CREATE TABLE boards (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    owner_id          BIGINT UNSIGNED NOT NULL,
    title             VARCHAR(100)    NOT NULL,
    description       VARCHAR(500)    NULL,
    background_type   VARCHAR(20)     NOT NULL DEFAULT 'COLOR',
    background_color  VARCHAR(20)     NULL DEFAULT '#0079BF',
    background_image  VARCHAR(500)    NULL,
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                               ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_boards PRIMARY KEY (id),

    CONSTRAINT fk_boards_owner
        FOREIGN KEY (owner_id)
        REFERENCES users (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT chk_boards_title_not_blank
        CHECK (CHAR_LENGTH(TRIM(title)) > 0),

    CONSTRAINT chk_boards_background_type
        CHECK (background_type IN ('COLOR', 'IMAGE')),

    CONSTRAINT chk_boards_background_value
        CHECK (
            (
                background_type = 'COLOR'
                AND background_color IS NOT NULL
                AND CHAR_LENGTH(TRIM(background_color)) > 0
                AND background_image IS NULL
            )
            OR
            (
                background_type = 'IMAGE'
                AND background_image IS NOT NULL
                AND CHAR_LENGTH(TRIM(background_image)) > 0
            )
        )
) ENGINE = InnoDB;

CREATE INDEX idx_boards_owner_updated
    ON boards (owner_id, updated_at);

-- ============================================================
-- 5. BOARD_LISTS
-- Danh sách công việc nằm trong một Board.
-- position bắt đầu từ 0 và được quản lý tại tầng Service.
-- ============================================================
CREATE TABLE board_lists (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    board_id    BIGINT UNSIGNED NOT NULL,
    title       VARCHAR(100)    NOT NULL,
    position    INT UNSIGNED    NOT NULL DEFAULT 0,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                         ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_board_lists PRIMARY KEY (id),

    CONSTRAINT fk_board_lists_board
        FOREIGN KEY (board_id)
        REFERENCES boards (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT chk_board_lists_title_not_blank
        CHECK (CHAR_LENGTH(TRIM(title)) > 0)
) ENGINE = InnoDB;

CREATE INDEX idx_board_lists_board_position
    ON board_lists (board_id, position);

-- ============================================================
-- 6. CARDS
-- Card thuộc một List. Card chỉ được di chuyển giữa các List
-- cùng Board; quy tắc này phải được kiểm tra tại tầng Service.
-- ============================================================
CREATE TABLE cards (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    list_id       BIGINT UNSIGNED NOT NULL,
    title         VARCHAR(150)    NOT NULL,
    description   TEXT            NULL,
    position      INT UNSIGNED    NOT NULL DEFAULT 0,
    due_date      DATETIME        NULL,
    completed     BOOLEAN         NOT NULL DEFAULT FALSE,
    completed_at  DATETIME        NULL,
    priority      VARCHAR(20)     NOT NULL DEFAULT 'MEDIUM',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                           ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_cards PRIMARY KEY (id),

    CONSTRAINT fk_cards_list
        FOREIGN KEY (list_id)
        REFERENCES board_lists (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT chk_cards_title_not_blank
        CHECK (CHAR_LENGTH(TRIM(title)) > 0),

    CONSTRAINT chk_cards_priority
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),

    CONSTRAINT chk_cards_completed_at
        CHECK (
            (completed = FALSE AND completed_at IS NULL)
            OR
            (completed = TRUE AND completed_at IS NOT NULL)
        )
) ENGINE = InnoDB;

CREATE INDEX idx_cards_list_position
    ON cards (list_id, position);

CREATE INDEX idx_cards_due_date
    ON cards (due_date);

CREATE INDEX idx_cards_priority
    ON cards (priority);

-- ============================================================
-- 7. LABELS
-- Label thuộc User và có thể tái sử dụng trên nhiều Board/Card
-- của chính User đó.
-- ============================================================
CREATE TABLE labels (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED NOT NULL,
    name        VARCHAR(50)     NOT NULL,
    color       VARCHAR(20)     NOT NULL,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                         ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_labels PRIMARY KEY (id),
    CONSTRAINT uq_labels_user_name UNIQUE (user_id, name),

    CONSTRAINT fk_labels_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT chk_labels_name_not_blank
        CHECK (CHAR_LENGTH(TRIM(name)) > 0),

    CONSTRAINT chk_labels_color_not_blank
        CHECK (CHAR_LENGTH(TRIM(color)) > 0)
) ENGINE = InnoDB;

CREATE INDEX idx_labels_user_id
    ON labels (user_id);

-- ============================================================
-- 8. CARD_LABELS
-- Quan hệ nhiều-nhiều giữa cards và labels.
-- Service phải bảo đảm Label thuộc cùng User sở hữu Board của Card.
-- ============================================================
CREATE TABLE card_labels (
    card_id   BIGINT UNSIGNED NOT NULL,
    label_id  BIGINT UNSIGNED NOT NULL,

    CONSTRAINT pk_card_labels PRIMARY KEY (card_id, label_id),

    CONSTRAINT fk_card_labels_card
        FOREIGN KEY (card_id)
        REFERENCES cards (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT fk_card_labels_label
        FOREIGN KEY (label_id)
        REFERENCES labels (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE INDEX idx_card_labels_label_id
    ON card_labels (label_id);

-- ============================================================
-- 9. COMMENTS
-- Bình luận trên Card. user_id xác định tác giả bình luận.
-- ============================================================
CREATE TABLE comments (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    card_id     BIGINT UNSIGNED NOT NULL,
    user_id     BIGINT UNSIGNED NOT NULL,
    content     TEXT            NOT NULL,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                         ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_comments PRIMARY KEY (id),

    CONSTRAINT fk_comments_card
        FOREIGN KEY (card_id)
        REFERENCES cards (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT fk_comments_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT chk_comments_content_not_blank
        CHECK (CHAR_LENGTH(TRIM(content)) > 0)
) ENGINE = InnoDB;

CREATE INDEX idx_comments_card_created
    ON comments (card_id, created_at);

CREATE INDEX idx_comments_user_id
    ON comments (user_id);

-- ============================================================
-- DỮ LIỆU KHỞI TẠO
-- Không tạo sẵn tài khoản vì mật khẩu phải được BCrypt tại ứng dụng.
-- ============================================================
INSERT INTO roles (id, name, description)
VALUES
    (1, 'ADMIN', 'Quản trị viên hệ thống'),
    (2, 'USER',  'Người dùng thông thường');

-- ============================================================
-- KẾT THÚC SCRIPT
-- ============================================================
