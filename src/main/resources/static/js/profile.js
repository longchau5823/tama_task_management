import { api, ApiError } from "./api.js";
import {
    getErrorMessage,
    showErrorToast,
    showSuccessToast
} from "./toast.js";

const DEFAULT_AVATAR_PATH = "/images/default-avatar.svg";
const MAX_AVATAR_SIZE = 2 * 1024 * 1024;
const ACCEPTED_AVATAR_TYPES = new Set(["image/jpeg", "image/png"]);

const pageMessage = document.querySelector("#profile-page-message");
const loading = document.querySelector("#profile-loading");
const content = document.querySelector("#profile-content");
const username = document.querySelector("#profile-username");
const createdAt = document.querySelector("#profile-created-at");
const updatedAt = document.querySelector("#profile-updated-at");
const avatarPreview = document.querySelector("#profile-avatar-preview");
const avatarFileInput = document.querySelector("#profile-avatar-file");
const avatarSelect = document.querySelector("#profile-avatar-select");
const avatarSubmit = document.querySelector("#profile-avatar-submit");
const avatarReset = document.querySelector("#profile-avatar-reset");
const avatarFileName = document.querySelector("#profile-avatar-file-name");
const avatarError = document.querySelector("#profile-avatar-error");

const emailForm = document.querySelector("#profile-email-form");
const emailInput = document.querySelector("#profile-email");
const emailSubmit = document.querySelector("#profile-email-submit");

const passwordForm = document.querySelector("#profile-password-form");
const currentPassword = document.querySelector("#current-password");
const newPassword = document.querySelector("#new-password");
const confirmPassword = document.querySelector("#confirm-password");
const passwordSubmit = document.querySelector("#profile-password-submit");
const showPasswords = document.querySelector("#show-profile-passwords");

let currentProfile = null;
let selectedAvatarFile = null;
let avatarPreviewUrl = null;
let avatarBusy = false;

function errorMessage(error, fallback) {
    return getErrorMessage(error, fallback);
}

function showPageMessage(text, type) {
    const element = pageMessage;
    element.textContent = text;
    element.className = `notice notice-${type}`;
    element.hidden = false;
}

function hidePageMessage() {
    const element = pageMessage;
    element.textContent = "";
    element.hidden = true;
}

function setBusy(button, busy, busyText) {
    if (!button.dataset.defaultText) {
        button.dataset.defaultText = button.textContent.trim();
    }
    button.disabled = busy;
    button.textContent = busy ? busyText : button.dataset.defaultText;
}

function formatDate(value) {
    if (!value) {
        return "Chưa có dữ liệu";
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return "Chưa có dữ liệu";
    }
    return new Intl.DateTimeFormat("vi-VN", {
        dateStyle: "medium",
        timeStyle: "short"
    }).format(date);
}

function avatarPath(profile) {
    return profile?.avatarPath || DEFAULT_AVATAR_PATH;
}

function updateTopbarAvatar(profile) {
    const topbarAvatar = document.querySelector("#account-menu-avatar");
    if (topbarAvatar) {
        topbarAvatar.src = avatarPath(profile);
        topbarAvatar.alt = profile?.username
            ? `Avatar của ${profile.username}`
            : "Avatar người dùng";
    }
    window.dispatchEvent(new CustomEvent("tama:avatar-updated", {
        detail: {
            avatarPath: avatarPath(profile),
            username: profile?.username || ""
        }
    }));
}

function showAvatarError(message) {
    avatarError.textContent = message;
    avatarError.hidden = false;
}

function hideAvatarError() {
    avatarError.textContent = "";
    avatarError.hidden = true;
}

function revokeAvatarPreviewUrl() {
    if (avatarPreviewUrl) {
        URL.revokeObjectURL(avatarPreviewUrl);
        avatarPreviewUrl = null;
    }
}

function setAvatarControlsBusy(busy) {
    avatarBusy = busy;
    avatarSelect.disabled = busy;
    avatarFileInput.disabled = busy;
    avatarSubmit.disabled = busy || selectedAvatarFile === null;
    avatarSubmit.textContent = busy ? "Đang lưu..." : "Lưu ảnh";
    avatarReset.disabled = busy
        || avatarPath(currentProfile) === DEFAULT_AVATAR_PATH;
}

function showStoredAvatar() {
    avatarPreview.src = avatarPath(currentProfile);
    avatarPreview.alt = currentProfile?.username
        ? `Ảnh đại diện của ${currentProfile.username}`
        : "Ảnh đại diện hiện tại";
}

function clearAvatarSelection({ restoreStoredAvatar = true } = {}) {
    revokeAvatarPreviewUrl();
    selectedAvatarFile = null;
    avatarFileInput.value = "";
    avatarFileName.textContent = "";
    if (restoreStoredAvatar) {
        showStoredAvatar();
    }
    setAvatarControlsBusy(avatarBusy);
}

function isAcceptedAvatarFile(file) {
    if (file.type) {
        return ACCEPTED_AVATAR_TYPES.has(file.type);
    }
    return /\.(?:jpe?g|png)$/i.test(file.name);
}

function renderProfile(profile) {
    currentProfile = profile;
    username.textContent = profile.username;
    emailInput.value = profile.email;
    createdAt.textContent = formatDate(profile.createdAt);
    updatedAt.textContent = formatDate(profile.updatedAt);
    if (!selectedAvatarFile) {
        showStoredAvatar();
    }
    updateTopbarAvatar(profile);
    setAvatarControlsBusy(avatarBusy);
}

async function loadProfile() {
    hidePageMessage();
    loading.hidden = false;
    content.hidden = true;

    try {
        const profile = await api.get("/api/auth/me");
        renderProfile(profile);
        content.hidden = false;
    } catch (error) {
        showPageMessage(errorMessage(error, "Không thể tải thông tin tài khoản."), "error");
        if (!(error instanceof ApiError)) {
            console.error(error);
        }
    } finally {
        loading.hidden = true;
    }
}

avatarSelect.addEventListener("click", () => {
    if (!avatarBusy) {
        avatarFileInput.click();
    }
});

avatarFileInput.addEventListener("change", () => {
    const file = avatarFileInput.files?.[0];
    hideAvatarError();
    if (!file) {
        clearAvatarSelection();
        return;
    }
    if (!isAcceptedAvatarFile(file)) {
        showAvatarError("Ảnh đại diện chỉ hỗ trợ định dạng JPEG hoặc PNG.");
        clearAvatarSelection();
        return;
    }
    if (file.size > MAX_AVATAR_SIZE) {
        showAvatarError("Ảnh đại diện không được vượt quá 2 MB.");
        clearAvatarSelection();
        return;
    }

    revokeAvatarPreviewUrl();
    selectedAvatarFile = file;
    avatarPreviewUrl = URL.createObjectURL(file);
    avatarPreview.src = avatarPreviewUrl;
    avatarPreview.alt = `Xem trước ảnh đại diện ${file.name}`;
    avatarFileName.textContent = file.name;
    setAvatarControlsBusy(false);
});

avatarPreview.addEventListener("error", () => {
    if (avatarPreviewUrl) {
        showAvatarError("Không thể hiển thị file ảnh đã chọn.");
        clearAvatarSelection();
        return;
    }
    if (!avatarPreview.src.endsWith(DEFAULT_AVATAR_PATH)) {
        avatarPreview.src = DEFAULT_AVATAR_PATH;
    }
});

avatarSubmit.addEventListener("click", async () => {
    if (!selectedAvatarFile || avatarBusy) {
        return;
    }

    const formData = new FormData();
    formData.append("file", selectedAvatarFile, selectedAvatarFile.name);
    hideAvatarError();
    setAvatarControlsBusy(true);

    try {
        const profile = await api.post("/api/profile/avatar", formData);
        clearAvatarSelection({ restoreStoredAvatar: false });
        renderProfile(profile);
        showSuccessToast("Cập nhật ảnh đại diện thành công.");
    } catch (error) {
        const message = errorMessage(error, "Không thể cập nhật ảnh đại diện.");
        showAvatarError(message);
        showErrorToast(message);
        if (!(error instanceof ApiError)) {
            console.error(error);
        }
    } finally {
        setAvatarControlsBusy(false);
    }
});

avatarReset.addEventListener("click", async () => {
    if (avatarBusy || avatarPath(currentProfile) === DEFAULT_AVATAR_PATH) {
        return;
    }

    hideAvatarError();
    setAvatarControlsBusy(true);
    try {
        const profile = await api.delete("/api/profile/avatar");
        clearAvatarSelection({ restoreStoredAvatar: false });
        renderProfile(profile);
        showSuccessToast("Đã khôi phục ảnh đại diện mặc định.");
    } catch (error) {
        const message = errorMessage(error, "Không thể khôi phục ảnh đại diện.");
        showAvatarError(message);
        showErrorToast(message);
        if (!(error instanceof ApiError)) {
            console.error(error);
        }
    } finally {
        setAvatarControlsBusy(false);
    }
});

window.addEventListener("pagehide", revokeAvatarPreviewUrl);

emailForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!emailForm.reportValidity() || emailSubmit.disabled) {
        return;
    }

    setBusy(emailSubmit, true, "Đang lưu...");

    try {
        const profile = await api.patch("/api/profile", { email: emailInput.value });
        renderProfile(profile);
        showSuccessToast("Cập nhật email thành công.");
    } catch (error) {
        showErrorToast(errorMessage(error, "Không thể cập nhật email."));
        if (!(error instanceof ApiError)) {
            console.error(error);
        }
    } finally {
        setBusy(emailSubmit, false, "");
    }
});

function setPasswordVisibility(visible) {
    const type = visible ? "text" : "password";
    currentPassword.type = type;
    newPassword.type = type;
    confirmPassword.type = type;
}

showPasswords.addEventListener("change", () => {
    setPasswordVisibility(showPasswords.checked);
});

passwordForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!passwordForm.reportValidity() || passwordSubmit.disabled) {
        return;
    }

    setBusy(passwordSubmit, true, "Đang đổi...");

    try {
        await api.patch("/api/profile/password", {
            currentPassword: currentPassword.value,
            newPassword: newPassword.value,
            confirmPassword: confirmPassword.value
        });
        passwordForm.reset();
        setPasswordVisibility(false);
        showSuccessToast("Đổi mật khẩu thành công.");
    } catch (error) {
        showErrorToast(errorMessage(error, "Không thể đổi mật khẩu."));
        if (!(error instanceof ApiError)) {
            console.error(error);
        }
    } finally {
        setBusy(passwordSubmit, false, "");
    }
});

loadProfile();
