import { api, ApiError } from "./api.js";
import { createCardManager } from "./card.js";
import {
    getErrorMessage,
    showErrorToast,
    showSuccessToast
} from "./toast.js";

const page = document.querySelector(".board-page");
const boardId = page.dataset.boardId;
const boardTitle = document.querySelector("#board-title");
const boardDescription = document.querySelector("#board-description");
const canvas = document.querySelector("#board-canvas");
const pageMessage = document.querySelector("#board-detail-message");
const listMessage = document.querySelector("#board-list-message");
const loading = document.querySelector("#board-lists-loading");
const empty = document.querySelector("#board-lists-empty");
const listScroller = document.querySelector("#board-list-scroll");
const track = document.querySelector("#board-list-track");
const dialog = document.querySelector("#board-list-dialog");
const form = document.querySelector("#board-list-form");
const dialogTitle = document.querySelector("#board-list-dialog-title");
const listTitleInput = document.querySelector("#board-list-title");
const saveButton = document.querySelector("#save-list-button");

let boardLists = [];
let reordering = false;
let draggedListId = null;

document.body.classList.add("board-mode");

const cardManager = createCardManager({
    onChange: renderBoardLists,
    errorMessage,
    isListReordering: () => reordering || draggedListId !== null
});

function useListTerm(text) {
    return typeof text === "string" ? text.replaceAll("BoardList", "List") : text;
}

function errorMessage(error, fallback) {
    return useListTerm(getErrorMessage(error, fallback));
}

function showPageError(error) {
    pageMessage.textContent = errorMessage(error, "Không thể tải thông tin Board.");
    pageMessage.className = "notice notice-error";
    pageMessage.hidden = false;
    boardTitle.textContent = "Không tìm thấy Board";
    if (!(error instanceof ApiError)) {
        console.error(error);
    }
}

function showListMessage(text, type) {
    listMessage.textContent = text;
    listMessage.className = `notice notice-${type}`;
    listMessage.hidden = false;
}

function hideListMessage() {
    listMessage.hidden = true;
    listMessage.textContent = "";
}

function applyBackground(board) {
    if (board.backgroundType === "IMAGE" && board.backgroundImage) {
        canvas.style.backgroundColor = "#27324a";
        canvas.style.backgroundImage =
            `linear-gradient(rgba(17, 24, 39, 0.3), rgba(17, 24, 39, 0.62)), url(${JSON.stringify(board.backgroundImage)})`;
        return;
    }
    canvas.style.backgroundColor = board.backgroundColor || "#4F46E5";
    canvas.style.backgroundImage = "";
}

function createDeleteButton(boardList) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "list-delete-button";
    button.title = `Xóa List ${boardList.title}`;
    button.setAttribute("aria-label", `Xóa List ${boardList.title}`);
    button.disabled = reordering || cardManager.isDraggingOrMoving();

    const icon = document.createElement("span");
    icon.setAttribute("aria-hidden", "true");
    icon.textContent = "−";
    button.append(icon);
    button.addEventListener("click", () => deleteBoardList(boardList));
    return button;
}

function startInlineTitleEdit(card, boardList) {
    if (reordering || cardManager.isDraggingOrMoving()) {
        return;
    }

    const titleButton = card.querySelector(".board-list-title-button");
    const titleForm = document.createElement("form");
    titleForm.className = "board-list-title-form";

    const titleInput = document.createElement("input");
    titleInput.type = "text";
    titleInput.value = boardList.title;
    titleInput.maxLength = 100;
    titleInput.required = true;
    titleInput.setAttribute("aria-label", `Đổi tên List ${boardList.title}`);
    titleForm.append(titleInput);

    card.draggable = false;
    titleButton.replaceWith(titleForm);
    titleInput.focus();
    titleInput.select();

    let finished = false;

    async function saveInlineTitle() {
        if (finished) {
            return;
        }
        finished = true;

        const title = titleInput.value.trim();
        if (!title) {
            renderBoardLists();
            showErrorToast("Tên List không được để trống.");
            return;
        }
        if (title === boardList.title) {
            renderBoardLists();
            return;
        }

        titleInput.disabled = true;
        try {
            const updated = await api.put(`/api/lists/${boardList.id}`, { title });
            boardLists = boardLists.map((item) => item.id === updated.id ? updated : item);
            renderBoardLists();
            showSuccessToast("Đổi tên danh sách thành công.");
        } catch (error) {
            renderBoardLists();
            showErrorToast(errorMessage(error, "Không thể đổi tên danh sách."));
            if (!(error instanceof ApiError)) {
                console.error(error);
            }
        }
    }

    titleForm.addEventListener("submit", (event) => {
        event.preventDefault();
        saveInlineTitle();
    });
    titleInput.addEventListener("blur", saveInlineTitle);
    titleInput.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            finished = true;
            renderBoardLists();
        }
    });
}

function createBoardListCard(boardList) {
    const card = document.createElement("article");
    card.className = "board-list-card";
    card.dataset.listId = String(boardList.id);
    card.draggable = !reordering && !cardManager.isDraggingOrMoving();
    card.title = "Kéo để thay đổi thứ tự List";
    let listDragArmed = false;

    const header = document.createElement("div");
    header.className = "board-list-card-header";

    const titleButton = document.createElement("button");
    titleButton.type = "button";
    titleButton.className = "board-list-title-button";
    titleButton.textContent = boardList.title;
    titleButton.title = `Nhấn để đổi tên List ${boardList.title}`;
    titleButton.setAttribute("aria-label", `Đổi tên List ${boardList.title}`);
    titleButton.disabled = reordering || cardManager.isDraggingOrMoving();
    titleButton.addEventListener("click", () => startInlineTitleEdit(card, boardList));

    const actions = document.createElement("div");
    actions.className = "board-list-actions";
    actions.append(createDeleteButton(boardList));

    const cardArea = document.createElement("div");
    cardArea.className = "board-list-card-area";
    cardManager.renderBoardList(boardList, cardArea);

    card.addEventListener("pointerdown", (event) => {
        listDragArmed = event.pointerType === "mouse"
            && event.button === 0
            && !reordering
            && !cardManager.isDraggingOrMoving()
            && !event.target.closest(".task-card, button, input, form, textarea, select");
    });
    card.addEventListener("pointerup", () => {
        if (draggedListId === null) {
            listDragArmed = false;
        }
    });
    card.addEventListener("pointercancel", () => {
        listDragArmed = false;
    });
    card.addEventListener("dragstart", (event) => {
        if (!listDragArmed || event.target.closest(".task-card")
                || reordering || cardManager.isDraggingOrMoving()
                || event.target.closest("button, input, form, textarea, select")) {
            event.preventDefault();
            return;
        }
        draggedListId = boardList.id;
        event.dataTransfer.effectAllowed = "move";
        event.dataTransfer.setData("text/plain", String(boardList.id));
        card.classList.add("is-dragging");
        card.setAttribute("aria-grabbed", "true");
        track.classList.add("is-reordering");
    });
    card.addEventListener("dragend", () => {
        listDragArmed = false;
        if (draggedListId !== boardList.id) {
            return;
        }
        card.classList.remove("is-dragging");
        card.removeAttribute("aria-grabbed");
        track.classList.remove("is-reordering");
        if (draggedListId !== null) {
            draggedListId = null;
            renderBoardLists();
        }
    });

    header.append(titleButton, actions);
    card.append(header, cardArea);
    return card;
}

function renderBoardLists() {
    cardManager.cancelInteraction();
    loading.hidden = true;
    track.replaceChildren();
    empty.hidden = boardLists.length !== 0;
    track.hidden = boardLists.length === 0;

    for (const boardList of boardLists) {
        track.append(createBoardListCard(boardList));
    }
}

function openCreateDialog() {
    form.reset();
    dialogTitle.textContent = "Tạo List";
    saveButton.textContent = "Tạo List";
    hideListMessage();
    dialog.showModal();
    listTitleInput.focus();
}

function closeDialog() {
    dialog.close();
}

async function saveBoardList(event) {
    event.preventDefault();
    if (!form.reportValidity()) {
        return;
    }

    const body = { title: listTitleInput.value };
    saveButton.disabled = true;

    try {
        const created = await api.post(`/api/boards/${boardId}/lists`, body);
        boardLists.push(created);
        boardLists.sort((left, right) => left.position - right.position);
        cardManager.initializeList(created.id);
        renderBoardLists();
        closeDialog();
        showSuccessToast("Tạo danh sách thành công.");
    } catch (error) {
        showErrorToast(errorMessage(error, "Không thể tạo danh sách."));
        if (!(error instanceof ApiError)) {
            console.error(error);
        }
    } finally {
        saveButton.disabled = false;
    }
}

async function deleteBoardList(boardList) {
    const confirmed = window.confirm(
        `Xóa danh sách "${boardList.title}"? Tất cả thẻ công việc bên trong cũng sẽ bị xóa và không thể khôi phục.`
    );
    if (!confirmed) {
        return;
    }

    try {
        await api.delete(`/api/lists/${boardList.id}`);
        boardLists = await api.get(`/api/boards/${boardId}/lists`);
        cardManager.removeList(boardList.id);
        renderBoardLists();
        showSuccessToast("Xóa danh sách thành công.");
    } catch (error) {
        showErrorToast(errorMessage(error, "Không thể xóa danh sách."));
        if (!(error instanceof ApiError)) {
            console.error(error);
        }
    }
}

async function reorderBoardLists(orderedListIds) {
    const currentListIds = boardLists.map((item) => item.id);
    if (reordering || cardManager.isDraggingOrMoving()
            || orderedListIds.every((id, index) => id === currentListIds[index])) {
        renderBoardLists();
        return;
    }

    const previousLists = boardLists;
    const listsById = new Map(boardLists.map((item) => [item.id, item]));
    boardLists = orderedListIds.map((id) => listsById.get(id));
    reordering = true;
    renderBoardLists();

    try {
        boardLists = await api.patch(`/api/boards/${boardId}/lists/reorder`, {
            orderedListIds
        });
    } catch (error) {
        try {
            boardLists = await api.get(`/api/boards/${boardId}/lists`);
        } catch (synchronizationError) {
            boardLists = previousLists;
            if (!(synchronizationError instanceof ApiError)) {
                console.error(synchronizationError);
            }
        }
        showErrorToast(errorMessage(
            error,
            "Không thể thay đổi thứ tự danh sách. Vui lòng thử lại."
        ));
        if (!(error instanceof ApiError)) {
            console.error(error);
        }
    } finally {
        reordering = false;
        renderBoardLists();
    }
}

async function loadBoardLists() {
    try {
        boardLists = await api.get(`/api/boards/${boardId}/lists`);
        renderBoardLists();
        await cardManager.loadCards(boardLists);
        renderBoardLists();
    } catch (error) {
        loading.hidden = true;
        showListMessage(errorMessage(error, "Không thể tải List."), "error");
        if (!(error instanceof ApiError)) {
            console.error(error);
        }
    }
}

async function loadBoardPage() {
    try {
        const board = await api.get(`/api/boards/${boardId}`);
        document.title = `${board.title} · TaMa`;
        boardTitle.textContent = board.title;
        boardDescription.textContent = board.description || "";
        boardDescription.hidden = !board.description;
        applyBackground(board);
        canvas.hidden = false;
        await loadBoardLists();
    } catch (error) {
        showPageError(error);
    }
}

document.querySelector("#create-list-button").addEventListener("click", openCreateDialog);
document.querySelector("#close-list-dialog").addEventListener("click", closeDialog);
document.querySelector("#cancel-list-button").addEventListener("click", closeDialog);
form.addEventListener("submit", saveBoardList);
dialog.addEventListener("click", (event) => {
    if (event.target === dialog) {
        closeDialog();
    }
});
listScroller.addEventListener("dragover", (event) => {
    if (draggedListId === null || reordering) {
        return;
    }
    event.preventDefault();
    event.dataTransfer.dropEffect = "move";

    const draggingCard = track.querySelector(".is-dragging");
    if (!draggingCard) {
        return;
    }

    const otherCards = [...track.querySelectorAll(".board-list-card:not(.is-dragging)")];
    const insertBefore = otherCards.find((card) => {
        const bounds = card.getBoundingClientRect();
        return event.clientX < bounds.left + bounds.width / 2;
    });

    if (insertBefore) {
        track.insertBefore(draggingCard, insertBefore);
    } else {
        track.append(draggingCard);
    }

    const scrollerBounds = listScroller.getBoundingClientRect();
    if (event.clientX < scrollerBounds.left + 48) {
        listScroller.scrollLeft -= 14;
    } else if (event.clientX > scrollerBounds.right - 48) {
        listScroller.scrollLeft += 14;
    }
});
listScroller.addEventListener("drop", (event) => {
    if (draggedListId === null || reordering) {
        return;
    }
    event.preventDefault();
    const orderedListIds = [...track.querySelectorAll(".board-list-card")]
            .map((card) => Number(card.dataset.listId));
    draggedListId = null;
    track.classList.remove("is-reordering");
    reorderBoardLists(orderedListIds);
});

loadBoardPage();
