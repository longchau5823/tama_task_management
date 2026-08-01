import { ApiError } from "./api.js";

const TOAST_DURATION = 3000;
const TOAST_EXIT_DURATION = 180;
const TOAST_TYPES = new Set(["success", "error", "warning", "info"]);
const toastTimers = new WeakMap();
const activeToasts = new Map();

const toastIcons = {
    success: "✓",
    error: "!",
    warning: "⚠",
    info: "i"
};

function getToastContainer() {
    let container = document.querySelector("#toast-container");
    if (container) {
        return container;
    }

    container = document.createElement("div");
    container.id = "toast-container";
    container.className = "toast-container";
    container.setAttribute("aria-live", "polite");
    container.setAttribute("aria-atomic", "false");
    document.body.append(container);
    return container;
}

function clearToastTimers(toast) {
    const timers = toastTimers.get(toast);
    if (!timers) {
        return;
    }
    window.clearTimeout(timers.dismiss);
    window.clearTimeout(timers.remove);
    toastTimers.delete(toast);
}

function removeToast(toast) {
    clearToastTimers(toast);
    const key = toast.dataset.toastKey;
    if (key && activeToasts.get(key) === toast) {
        activeToasts.delete(key);
    }
    toast.remove();
}

export function dismissToast(toast) {
    if (!(toast instanceof HTMLElement) || toast.dataset.closing === "true") {
        return;
    }

    toast.dataset.closing = "true";
    const currentTimers = toastTimers.get(toast);
    if (currentTimers) {
        window.clearTimeout(currentTimers.dismiss);
    }
    toast.classList.remove("is-visible");
    toast.classList.add("is-leaving");

    const removeTimer = window.setTimeout(() => removeToast(toast), TOAST_EXIT_DURATION);
    toastTimers.set(toast, {
        dismiss: null,
        remove: removeTimer
    });
}

function normalizeDuration(duration) {
    const numericDuration = Number(duration);
    if (!Number.isFinite(numericDuration)) {
        return TOAST_DURATION;
    }
    return Math.min(TOAST_DURATION, Math.max(1000, numericDuration));
}

export function showToast({
    type = "info",
    message,
    duration = TOAST_DURATION
} = {}) {
    const normalizedType = TOAST_TYPES.has(type) ? type : "info";
    const normalizedMessage = String(message || "").trim();
    if (!normalizedMessage) {
        return null;
    }

    const key = `${normalizedType}:${normalizedMessage}`;
    const duplicate = activeToasts.get(key);
    if (duplicate?.isConnected) {
        return duplicate;
    }

    const normalizedDuration = normalizeDuration(duration);
    const toast = document.createElement("article");
    toast.className = `toast toast-${normalizedType}`;
    toast.dataset.toastKey = key;
    toast.style.setProperty("--toast-duration", `${normalizedDuration}ms`);
    toast.setAttribute("role", normalizedType === "error" ? "alert" : "status");

    const icon = document.createElement("span");
    icon.className = "toast-icon";
    icon.setAttribute("aria-hidden", "true");
    icon.textContent = toastIcons[normalizedType];

    const messageElement = document.createElement("p");
    messageElement.className = "toast-message";
    messageElement.textContent = normalizedMessage;

    const closeButton = document.createElement("button");
    closeButton.type = "button";
    closeButton.className = "toast-close";
    closeButton.setAttribute("aria-label", "Đóng thông báo");
    closeButton.textContent = "×";
    closeButton.addEventListener("click", () => dismissToast(toast), { once: true });

    const timeline = document.createElement("span");
    timeline.className = "toast-timeline";
    timeline.setAttribute("aria-hidden", "true");

    toast.append(icon, messageElement, closeButton, timeline);
    getToastContainer().append(toast);
    activeToasts.set(key, toast);

    window.requestAnimationFrame(() => {
        toast.classList.add("is-visible");
    });

    const dismissTimer = window.setTimeout(() => dismissToast(toast), normalizedDuration);
    toastTimers.set(toast, {
        dismiss: dismissTimer,
        remove: null
    });
    return toast;
}

export function showSuccessToast(message, duration) {
    return showToast({ type: "success", message, duration });
}

export function showErrorToast(message, duration) {
    return showToast({ type: "error", message, duration });
}

export function showWarningToast(message, duration) {
    return showToast({ type: "warning", message, duration });
}

export function showInfoToast(message, duration) {
    return showToast({ type: "info", message, duration });
}

function isSafeApiMessage(message) {
    if (typeof message !== "string") {
        return false;
    }
    const normalized = message.trim();
    if (!normalized || normalized.length > 240 || /<[^>]*>/.test(normalized)) {
        return false;
    }
    return !/(?:\bException\b|stack\s*trace|SQLSTATE|Hibernate|JDBC|org\.springframework|java\.[a-z]|Request failed with status|Server returned invalid JSON|password\s*hash|_csrf)/i
        .test(normalized);
}

function validationMessage(error) {
    const validationErrors = error.payload?.validationErrors;
    if (!validationErrors || typeof validationErrors !== "object") {
        return "";
    }

    const messages = Object.values(validationErrors)
        .filter(isSafeApiMessage)
        .map((message) => message.trim());
    return messages.join(". ");
}

export function getErrorMessage(error, fallback) {
    if (error instanceof ApiError) {
        const validation = validationMessage(error);
        if (validation) {
            return validation;
        }
        if (error.status === 401) {
            return "Phiên đăng nhập đã hết hạn.";
        }
        if (error.status === 403) {
            return "Bạn không có quyền thực hiện thao tác này hoặc phiên xác thực không hợp lệ.";
        }
        if (isSafeApiMessage(error.message)) {
            return error.message.trim();
        }
        if (error.status === 400) {
            return "Dữ liệu nhập chưa hợp lệ.";
        }
    }

    if (error instanceof TypeError) {
        return "Kết nối tới máy chủ thất bại.";
    }
    return fallback;
}
