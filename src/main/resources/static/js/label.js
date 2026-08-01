import { api, ApiError } from "./api.js";
import {
    getErrorMessage,
    showErrorToast,
    showSuccessToast
} from "./toast.js";

const DEFAULT_LABEL_COLOR = "#3B82F6";
const LABEL_COLOR_PATTERN = /^#[0-9A-F]{6}$/;

function safeLabelColor(color) {
    const normalized = String(color || "").trim().toUpperCase();
    return LABEL_COLOR_PATTERN.test(normalized) ? normalized : DEFAULT_LABEL_COLOR;
}

export function getLabelTextColor(color) {
    const normalized = safeLabelColor(color);
    const red = Number.parseInt(normalized.slice(1, 3), 16);
    const green = Number.parseInt(normalized.slice(3, 5), 16);
    const blue = Number.parseInt(normalized.slice(5, 7), 16);
    return (red * 299 + green * 587 + blue * 114) / 1000 >= 150
        ? "#172033"
        : "#FFFFFF";
}

export function createLabelChip(label, className = "") {
    const chip = document.createElement("span");
    chip.className = ["label-chip", className].filter(Boolean).join(" ");
    chip.textContent = label.name;
    chip.title = label.name;
    const color = safeLabelColor(label.color);
    chip.style.backgroundColor = color;
    chip.style.color = getLabelTextColor(color);
    return chip;
}

function initializeLabelPage(page) {
    const loading = page.querySelector("#labels-loading");
    const message = page.querySelector("#labels-message");
    const empty = page.querySelector("#labels-empty");
    const grid = page.querySelector("#labels-grid");
    const dialog = page.querySelector("#label-dialog");
    const form = page.querySelector("#label-form");
    const dialogTitle = page.querySelector("#label-dialog-title");
    const idInput = page.querySelector("#label-id");
    const nameInput = page.querySelector("#label-name");
    const colorInput = page.querySelector("#label-color");
    const colorValue = page.querySelector("#label-color-value");
    const preview = page.querySelector("#label-preview-chip");
    const formMessage = page.querySelector("#label-form-message");
    const saveButton = page.querySelector("#save-label-button");

    let labels = [];

    function showPageError(error) {
        message.textContent = getErrorMessage(error, "Không thể tải danh sách nhãn.");
        message.hidden = false;
    }

    function hidePageError() {
        message.hidden = true;
        message.textContent = "";
    }

    function showFormError(text) {
        formMessage.textContent = text;
        formMessage.hidden = false;
    }

    function hideFormError() {
        formMessage.hidden = true;
        formMessage.textContent = "";
    }

    function updatePreview() {
        const color = safeLabelColor(colorInput.value);
        colorInput.value = color;
        colorValue.textContent = color;
        preview.replaceChildren(createLabelChip({
            name: nameInput.value.trim() || "Tên nhãn",
            color
        }));
    }

    function closeDialog() {
        dialog.close();
    }

    function openCreateDialog() {
        form.reset();
        idInput.value = "";
        colorInput.value = DEFAULT_LABEL_COLOR;
        dialogTitle.textContent = "Tạo nhãn";
        saveButton.textContent = "Tạo nhãn";
        hideFormError();
        updatePreview();
        dialog.showModal();
        nameInput.focus();
    }

    function openEditDialog(label) {
        form.reset();
        idInput.value = String(label.id);
        nameInput.value = label.name;
        colorInput.value = safeLabelColor(label.color);
        dialogTitle.textContent = "Chỉnh sửa nhãn";
        saveButton.textContent = "Lưu thay đổi";
        hideFormError();
        updatePreview();
        dialog.showModal();
        nameInput.focus();
    }

    function createLabelCard(label) {
        const article = document.createElement("article");
        article.className = "label-card";

        const chip = createLabelChip(label, "label-card-chip");
        const actions = document.createElement("div");
        actions.className = "label-card-actions";

        const editButton = document.createElement("button");
        editButton.type = "button";
        editButton.className = "button button-quiet";
        editButton.textContent = "Sửa";
        editButton.addEventListener("click", () => openEditDialog(label));

        const deleteButton = document.createElement("button");
        deleteButton.type = "button";
        deleteButton.className = "button button-quiet label-delete-button";
        deleteButton.textContent = "Xóa";
        deleteButton.addEventListener("click", () => deleteLabel(label, deleteButton));

        actions.append(editButton, deleteButton);
        article.append(chip, actions);
        return article;
    }

    function renderLabels() {
        grid.replaceChildren(...labels.map(createLabelCard));
        const hasLabels = labels.length > 0;
        grid.hidden = !hasLabels;
        empty.hidden = hasLabels;
    }

    async function loadLabels() {
        loading.hidden = false;
        empty.hidden = true;
        grid.hidden = true;
        hidePageError();
        try {
            labels = await api.get("/api/labels");
            renderLabels();
        } catch (error) {
            showPageError(error);
            if (!(error instanceof ApiError)) {
                console.error(error);
            }
        } finally {
            loading.hidden = true;
        }
    }

    async function saveLabel(event) {
        event.preventDefault();
        hideFormError();
        const name = nameInput.value.trim();
        if (!name) {
            showFormError("Tên nhãn không được để trống.");
            nameInput.focus();
            return;
        }
        if (!form.reportValidity()) {
            return;
        }

        const labelId = idInput.value;
        const body = {
            name,
            color: safeLabelColor(colorInput.value)
        };
        saveButton.disabled = true;

        try {
            if (labelId) {
                const updated = await api.put(`/api/labels/${labelId}`, body);
                labels = labels
                    .map((label) => label.id === updated.id ? updated : label)
                    .sort((left, right) => left.name.localeCompare(right.name, "vi"));
                showSuccessToast("Cập nhật nhãn thành công.");
            } else {
                const created = await api.post("/api/labels", body);
                labels = [...labels, created]
                    .sort((left, right) => left.name.localeCompare(right.name, "vi"));
                showSuccessToast("Tạo nhãn thành công.");
            }
            renderLabels();
            closeDialog();
        } catch (error) {
            showErrorToast(getErrorMessage(
                error,
                labelId ? "Không thể cập nhật nhãn." : "Không thể tạo nhãn."
            ));
            if (!(error instanceof ApiError)) {
                console.error(error);
            }
        } finally {
            saveButton.disabled = false;
        }
    }

    async function deleteLabel(label, button) {
        if (!window.confirm(
            `Xóa Label "${label.name}"? Label sẽ được gỡ khỏi tất cả Card và thao tác này không thể khôi phục.`
        )) {
            return;
        }

        button.disabled = true;
        try {
            await api.delete(`/api/labels/${label.id}`);
            labels = labels.filter((item) => item.id !== label.id);
            renderLabels();
            showSuccessToast("Xóa nhãn thành công.");
        } catch (error) {
            button.disabled = false;
            showErrorToast(getErrorMessage(error, "Không thể xóa nhãn."));
            if (!(error instanceof ApiError)) {
                console.error(error);
            }
        }
    }

    page.querySelector("#create-label-button").addEventListener("click", openCreateDialog);
    page.querySelector("#create-first-label-button").addEventListener("click", openCreateDialog);
    page.querySelector("#close-label-dialog").addEventListener("click", closeDialog);
    page.querySelector("#cancel-label-button").addEventListener("click", closeDialog);
    page.querySelector("#label-palette").addEventListener("click", (event) => {
        const colorButton = event.target.closest("[data-color]");
        if (!colorButton) {
            return;
        }
        colorInput.value = colorButton.dataset.color;
        updatePreview();
    });
    nameInput.addEventListener("input", updatePreview);
    colorInput.addEventListener("input", updatePreview);
    form.addEventListener("submit", saveLabel);
    dialog.addEventListener("click", (event) => {
        if (event.target === dialog) {
            closeDialog();
        }
    });

    void loadLabels();
}

const labelPage = document.querySelector("[data-label-page]");
if (labelPage) {
    initializeLabelPage(labelPage);
}
