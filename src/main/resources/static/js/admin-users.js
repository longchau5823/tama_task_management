import { api, ApiError } from "./api.js";
import {
    getErrorMessage,
    showErrorToast,
    showSuccessToast
} from "./toast.js";

const message = document.querySelector("#admin-users-message");
const loading = document.querySelector("#users-loading");
const empty = document.querySelector("#users-empty");
const tableWrapper = document.querySelector("#users-table-wrapper");
const tableBody = document.querySelector("#users-table-body");

let users = [];
let currentUser = null;

function showPageMessage(text, type) {
    message.textContent = text;
    message.className = `notice notice-${type}`;
    message.hidden = false;
}

function createCell(text) {
    const cell = document.createElement("td");
    cell.textContent = text;
    return cell;
}

function createBadge(text, className) {
    const badge = document.createElement("span");
    badge.className = className;
    badge.textContent = text;
    return badge;
}

function formatCreatedAt(value) {
    if (!value) {
        return "—";
    }
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? value : date.toLocaleString("vi-VN");
}

async function updateUser(user, url, body, successMessage) {
    try {
        const updated = await api.patch(url, body);
        users = users.map((item) => item.id === updated.id ? updated : item);
        showSuccessToast(successMessage);
    } catch (error) {
        showErrorToast(getErrorMessage(error, "Không thể cập nhật tài khoản."));
        if (!(error instanceof ApiError)) {
            console.error(error);
        }
    } finally {
        renderUsers();
    }
}

function createActions(user) {
    const cell = document.createElement("td");
    const actions = document.createElement("div");
    actions.className = "user-actions";

    const isCurrentUser = currentUser && user.id === currentUser.id;
    const hasAdminRole = user.roles.includes("ADMIN");

    const enabledButton = document.createElement("button");
    enabledButton.type = "button";
    enabledButton.className = `button button-small ${user.enabled ? "button-danger" : "button-secondary"}`;
    enabledButton.textContent = user.enabled ? "Khóa" : "Mở khóa";
    enabledButton.disabled = isCurrentUser;
    if (isCurrentUser) {
        enabledButton.title = "Bạn không thể tự khóa tài khoản đang đăng nhập";
    }
    enabledButton.addEventListener("click", async () => {
        const nextEnabled = !user.enabled;
        const action = nextEnabled ? "mở khóa" : "khóa";
        if (!window.confirm(`Bạn có chắc muốn ${action} tài khoản ${user.username}?`)) {
            return;
        }
        enabledButton.disabled = true;
        await updateUser(
            user,
            `/api/admin/users/${user.id}/enabled`,
            { enabled: nextEnabled },
            `Đã ${action} tài khoản ${user.username}.`
        );
    });

    const adminButton = document.createElement("button");
    adminButton.type = "button";
    adminButton.className = "button button-small button-secondary";
    adminButton.textContent = hasAdminRole ? "Gỡ ADMIN" : "Cấp ADMIN";
    adminButton.disabled = isCurrentUser;
    if (isCurrentUser) {
        adminButton.title = "Bạn không thể tự gỡ quyền ADMIN";
    }
    adminButton.addEventListener("click", async () => {
        const nextAdmin = !hasAdminRole;
        const action = nextAdmin ? "cấp quyền ADMIN cho" : "gỡ quyền ADMIN khỏi";
        if (!window.confirm(`Bạn có chắc muốn ${action} ${user.username}?`)) {
            return;
        }
        adminButton.disabled = true;
        await updateUser(
            user,
            `/api/admin/users/${user.id}/admin-role`,
            { admin: nextAdmin },
            `Đã cập nhật quyền của ${user.username}.`
        );
    });

    actions.append(enabledButton, adminButton);
    cell.append(actions);
    return cell;
}

function renderUsers() {
    tableBody.replaceChildren();
    loading.hidden = true;
    empty.hidden = users.length !== 0;
    tableWrapper.hidden = users.length === 0;

    for (const user of users) {
        const row = document.createElement("tr");
        row.append(
            createCell(String(user.id)),
            createCell(user.username),
            createCell(user.email)
        );

        const statusCell = document.createElement("td");
        statusCell.append(createBadge(
            user.enabled ? "Hoạt động" : "Đã khóa",
            `status-badge ${user.enabled ? "status-enabled" : "status-disabled"}`
        ));
        row.append(statusCell);

        const rolesCell = document.createElement("td");
        const displayedRole = user.roles.includes("ADMIN") ? "ADMIN" : "USER";
        rolesCell.append(createBadge(displayedRole, "role-badge"));
        row.append(rolesCell);
        row.append(createCell(formatCreatedAt(user.createdAt)));
        row.append(createActions(user));
        tableBody.append(row);
    }
}

async function loadUsers() {
    try {
        [users, currentUser] = await Promise.all([
            api.get("/api/admin/users"),
            api.get("/api/auth/me")
        ]);
        renderUsers();
    } catch (error) {
        loading.hidden = true;
        showPageMessage(
            getErrorMessage(error, "Không thể tải danh sách tài khoản."),
            "error"
        );
        if (!(error instanceof ApiError)) {
            console.error(error);
        }
    }
}

loadUsers();
