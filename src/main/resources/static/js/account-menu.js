import { api } from "./api.js";

const DEFAULT_AVATAR_PATH = "/images/default-avatar.svg";
const avatar = document.querySelector("#account-menu-avatar");

function useDefaultAvatar() {
    if (avatar.src.endsWith(DEFAULT_AVATAR_PATH)) {
        return;
    }
    avatar.src = DEFAULT_AVATAR_PATH;
}

function updateAvatar(avatarPath, username) {
    avatar.src = avatarPath || DEFAULT_AVATAR_PATH;
    avatar.alt = username ? `Avatar của ${username}` : "Avatar người dùng";
}

if (avatar) {
    avatar.addEventListener("error", useDefaultAvatar);
    window.addEventListener("tama:avatar-updated", (event) => {
        updateAvatar(event.detail?.avatarPath, event.detail?.username);
    });

    try {
        const user = await api.get("/api/auth/me");
        updateAvatar(user.avatarPath, user.username);
    } catch (error) {
        useDefaultAvatar();
        console.error("Không thể tải avatar tài khoản.", error);
    }
}
