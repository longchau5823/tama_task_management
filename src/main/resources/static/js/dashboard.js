import { api, ApiError } from "./api.js";
import {
    getErrorMessage,
    showErrorToast,
    showSuccessToast
} from "./toast.js";

const MAX_CUSTOM_IMAGE_SIZE = 5 * 1024 * 1024;
const ALLOWED_CUSTOM_IMAGE_TYPES = new Set(["image/jpeg", "image/png"]);

const message = document.querySelector("#board-message");
const loading = document.querySelector("#boards-loading");
const empty = document.querySelector("#boards-empty");
const grid = document.querySelector("#board-grid");
const dialog = document.querySelector("#board-dialog");
const form = document.querySelector("#board-form");
const dialogTitle = document.querySelector("#board-dialog-title");
const boardIdInput = document.querySelector("#board-id");
const titleInput = document.querySelector("#board-title");
const descriptionInput = document.querySelector("#board-description");
const colorInput = document.querySelector("#board-background-color");
const imageInput = document.querySelector("#board-background-image");
const customImageInput = document.querySelector("#board-custom-background");
const colorField = document.querySelector("#background-color-field");
const imageField = document.querySelector("#background-image-field");
const imageLibrary = document.querySelector("#board-background-library");
const imageThumbnails = [...document.querySelectorAll(".board-background-thumbnail")];
const customImageChoice = document.querySelector("#board-custom-background-choice");
const imagePreview = document.querySelector("#board-background-preview");
const imagePreviewElement = document.querySelector("#board-background-preview-image");
const imagePreviewName = document.querySelector("#board-background-preview-name");
const imageError = document.querySelector("#board-background-error");
const saveButton = document.querySelector("#save-board-button");
const libraryBackgrounds = new Set(
    imageThumbnails.map((button) => button.dataset.backgroundPath)
);

let boards = [];
let selectedImagePath = "";
let selectedCustomFile = null;
let customPreviewUrl = "";

function showPageMessage(text, type) {
    message.textContent = text;
    message.className = `notice notice-${type}`;
    message.hidden = false;
}

function hidePageMessage() {
    message.hidden = true;
    message.textContent = "";
}

function errorMessage(error, fallback) {
    return getErrorMessage(error, fallback);
}

function selectedBackgroundType() {
    return form.elements.backgroundType.value;
}

function clearImageError() {
    imageError.hidden = true;
    imageError.textContent = "";
}

function showImageError(text) {
    imageError.textContent = text;
    imageError.hidden = false;
}

function revokeCustomPreview() {
    if (customPreviewUrl) {
        URL.revokeObjectURL(customPreviewUrl);
        customPreviewUrl = "";
    }
}

function selectedLibraryLabel(path) {
    const selectedButton = imageThumbnails.find(
        (button) => button.dataset.backgroundPath === path
    );
    return selectedButton?.querySelector("span")?.textContent || "Ảnh thư viện";
}

function renderImageSelection() {
    imageInput.value = selectedImagePath;
    for (const button of imageThumbnails) {
        const selected = !selectedCustomFile
            && button.dataset.backgroundPath === selectedImagePath;
        button.classList.toggle("is-selected", selected);
        button.setAttribute("aria-pressed", String(selected));
    }

    const customSelected = Boolean(selectedCustomFile)
        || Boolean(selectedImagePath && !libraryBackgrounds.has(selectedImagePath));
    customImageChoice.classList.toggle("is-selected", customSelected);

    const previewSource = customPreviewUrl || selectedImagePath;
    imagePreview.hidden = !previewSource;
    if (!previewSource) {
        imagePreviewElement.removeAttribute("src");
        imagePreviewName.textContent = "";
        return;
    }

    imagePreviewElement.src = previewSource;
    imagePreviewName.textContent = selectedCustomFile
        ? selectedCustomFile.name
        : libraryBackgrounds.has(selectedImagePath)
            ? selectedLibraryLabel(selectedImagePath)
            : "Ảnh tùy chỉnh hiện tại";
}

function resetImageSelection(path = "") {
    revokeCustomPreview();
    selectedCustomFile = null;
    selectedImagePath = path;
    customImageInput.value = "";
    clearImageError();
    renderImageSelection();
}

function selectLibraryBackground(path) {
    if (!libraryBackgrounds.has(path)) {
        return;
    }
    resetImageSelection(path);
}

function ensureDefaultImageSelection() {
    if (!selectedImagePath && !selectedCustomFile && imageThumbnails.length > 0) {
        selectLibraryBackground(imageThumbnails[0].dataset.backgroundPath);
    }
}

function updateBackgroundFields() {
    const imageSelected = selectedBackgroundType() === "IMAGE";
    colorField.hidden = imageSelected;
    imageField.hidden = !imageSelected;
    colorInput.disabled = imageSelected;
    if (imageSelected) {
        ensureDefaultImageSelection();
    }
}

function openCreateDialog() {
    form.reset();
    boardIdInput.value = "";
    colorInput.value = "#4F46E5";
    resetImageSelection();
    dialogTitle.textContent = "Tạo Board";
    saveButton.textContent = "Tạo Board";
    updateBackgroundFields();
    hidePageMessage();
    dialog.showModal();
    titleInput.focus();
}

function openEditDialog(board) {
    form.reset();
    boardIdInput.value = String(board.id);
    titleInput.value = board.title;
    descriptionInput.value = board.description || "";
    form.elements.backgroundType.value = board.backgroundType;
    colorInput.value = board.backgroundColor || "#4F46E5";
    resetImageSelection(board.backgroundImage || "");
    dialogTitle.textContent = "Sửa Board";
    saveButton.textContent = "Lưu thay đổi";
    updateBackgroundFields();
    hidePageMessage();
    dialog.showModal();
    titleInput.focus();
}

function closeDialog() {
    revokeCustomPreview();
    dialog.close();
}

function applyBoardBackground(element, board) {
    if (board.backgroundType === "IMAGE" && board.backgroundImage) {
        element.style.backgroundColor = "#27324a";
        element.style.backgroundImage = `url(${JSON.stringify(board.backgroundImage)})`;
        return;
    }
    element.style.backgroundColor = board.backgroundColor || "#4F46E5";
    element.style.backgroundImage = "";
}

function createActionButton(text, className, action) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = className;
    button.textContent = text;
    button.addEventListener("click", (event) => {
        event.stopPropagation();
        action();
    });
    return button;
}

async function deleteBoard(board) {
    if (!window.confirm(`Bạn có chắc muốn xóa Board "${board.title}"? Dữ liệu bên trong cũng sẽ bị xóa.`)) {
        return;
    }

    try {
        await api.delete(`/api/boards/${board.id}`);
        boards = boards.filter((item) => item.id !== board.id);
        renderBoards();
        showSuccessToast("Xóa Board thành công.");
    } catch (error) {
        showErrorToast(errorMessage(error, "Không thể xóa Board."));
        if (!(error instanceof ApiError)) {
            console.error(error);
        }
    }
}

function createBoardCard(board) {
    const card = document.createElement("article");
    card.className = "board-card";
    card.tabIndex = 0;
    card.setAttribute("role", "link");
    card.setAttribute("aria-label", `Mở Board ${board.title}`);

    const cover = document.createElement("div");
    cover.className = "board-card-cover";
    applyBoardBackground(cover, board);

    const footer = document.createElement("div");
    footer.className = "board-card-footer";

    const title = document.createElement("h2");
    title.textContent = board.title;

    const actions = document.createElement("div");
    actions.className = "board-actions";
    actions.append(
        createActionButton("Sửa", "board-action", () => openEditDialog(board)),
        createActionButton("Xóa", "board-action board-action-danger", () => deleteBoard(board))
    );

    cover.append(actions);
    footer.append(title);
    card.append(cover, footer);

    const openBoard = () => {
        window.location.href = `/boards/${board.id}`;
    };
    card.addEventListener("click", openBoard);
    card.addEventListener("keydown", (event) => {
        if (event.target !== card) {
            return;
        }
        if (event.key === "Enter" || event.key === " ") {
            event.preventDefault();
            openBoard();
        }
    });
    return card;
}

function renderBoards() {
    loading.hidden = true;
    grid.replaceChildren();
    empty.hidden = boards.length !== 0;
    grid.hidden = boards.length === 0;

    for (const board of boards) {
        grid.append(createBoardCard(board));
    }
}

function sortBoards() {
    boards.sort((left, right) => {
        const leftTime = new Date(left.updatedAt).getTime() || 0;
        const rightTime = new Date(right.updatedAt).getTime() || 0;
        return rightTime - leftTime;
    });
}

function validateCustomImage(file) {
    const normalizedName = file.name.toLowerCase();
    const hasAllowedExtension = [".jpg", ".jpeg", ".png"].some(
        (extension) => normalizedName.endsWith(extension)
    );
    if ((file.type && !ALLOWED_CUSTOM_IMAGE_TYPES.has(file.type))
            || (!file.type && !hasAllowedExtension)) {
        return "Ảnh tùy chỉnh chỉ hỗ trợ định dạng JPEG hoặc PNG.";
    }
    if (file.size > MAX_CUSTOM_IMAGE_SIZE) {
        return "Ảnh tùy chỉnh không được vượt quá 5 MB.";
    }
    return "";
}

function selectCustomImage(file) {
    const validationError = validateCustomImage(file);
    if (validationError) {
        customImageInput.value = "";
        showImageError(validationError);
        return;
    }

    revokeCustomPreview();
    selectedCustomFile = file;
    selectedImagePath = "";
    customPreviewUrl = URL.createObjectURL(file);
    clearImageError();
    renderImageSelection();
}

async function uploadCustomImage() {
    const body = new FormData();
    body.append("file", selectedCustomFile);
    return api.post("/api/board-backgrounds", body);
}

function uploadedFileName(path) {
    const segments = String(path || "").split("/");
    return segments[segments.length - 1] || "";
}

async function cleanupUnusedUpload(path) {
    const fileName = uploadedFileName(path);
    if (!fileName) {
        return;
    }
    try {
        await api.delete(`/api/board-backgrounds/files/${encodeURIComponent(fileName)}`);
    } catch (error) {
        console.warn("Không thể dọn file ảnh nền chưa sử dụng.", error);
    }
}

async function saveBoard(event) {
    event.preventDefault();
    clearImageError();
    if (!form.reportValidity()) {
        return;
    }

    const backgroundType = selectedBackgroundType();
    if (backgroundType === "IMAGE" && !selectedCustomFile && !selectedImagePath) {
        showImageError("Vui lòng chọn ảnh từ thư viện hoặc chọn ảnh từ máy.");
        return;
    }

    const boardId = boardIdInput.value;
    const defaultButtonText = boardId ? "Lưu thay đổi" : "Tạo Board";
    let uploadedPath = "";
    saveButton.disabled = true;

    try {
        let backgroundImage = null;
        if (backgroundType === "IMAGE") {
            if (selectedCustomFile) {
                saveButton.textContent = "Đang tải ảnh...";
                const upload = await uploadCustomImage();
                uploadedPath = upload.path;
                backgroundImage = upload.path;
            } else {
                backgroundImage = selectedImagePath;
            }
        }

        saveButton.textContent = "Đang lưu...";
        const body = {
            title: titleInput.value,
            description: descriptionInput.value,
            backgroundType,
            backgroundColor: backgroundType === "COLOR" ? colorInput.value : null,
            backgroundImage
        };

        if (boardId) {
            const updated = await api.put(`/api/boards/${boardId}`, body);
            uploadedPath = "";
            boards = boards.map((board) => board.id === updated.id ? updated : board);
            showSuccessToast("Cập nhật Board thành công.");
        } else {
            const created = await api.post("/api/boards", body);
            uploadedPath = "";
            boards.push(created);
            showSuccessToast("Tạo Board thành công.");
        }
        sortBoards();
        renderBoards();
        closeDialog();
    } catch (error) {
        if (uploadedPath) {
            await cleanupUnusedUpload(uploadedPath);
        }
        showErrorToast(errorMessage(
            error,
            boardId ? "Không thể cập nhật Board." : "Không thể tạo Board."
        ));
        if (!(error instanceof ApiError)) {
            console.error(error);
        }
    } finally {
        saveButton.disabled = false;
        saveButton.textContent = defaultButtonText;
    }
}

async function loadBoards() {
    try {
        boards = await api.get("/api/boards");
        renderBoards();
    } catch (error) {
        loading.hidden = true;
        showPageMessage(errorMessage(error, "Không thể tải danh sách Board."), "error");
        if (!(error instanceof ApiError)) {
            console.error(error);
        }
    }
}

document.querySelector("#create-board-button").addEventListener("click", openCreateDialog);
document.querySelector("#empty-create-board-button").addEventListener("click", openCreateDialog);
document.querySelector("#close-board-dialog").addEventListener("click", closeDialog);
document.querySelector("#cancel-board-button").addEventListener("click", closeDialog);
imageLibrary.addEventListener("click", (event) => {
    const button = event.target.closest("[data-background-path]");
    if (button) {
        selectLibraryBackground(button.dataset.backgroundPath);
    }
});
customImageInput.addEventListener("change", () => {
    const [file] = customImageInput.files;
    if (file) {
        selectCustomImage(file);
    }
});
form.addEventListener("change", (event) => {
    if (event.target.name === "backgroundType") {
        updateBackgroundFields();
    }
});
form.addEventListener("submit", saveBoard);
dialog.addEventListener("click", (event) => {
    if (event.target === dialog) {
        closeDialog();
    }
});
dialog.addEventListener("close", revokeCustomPreview);

void loadBoards();
