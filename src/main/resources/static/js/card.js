import { api, ApiError } from "./api.js";
import {
    showErrorToast,
    showSuccessToast
} from "./toast.js";
import { createLabelChip } from "./label.js";
import { createCommentManager } from "./comment.js";

export function createCardManager({
    onChange,
    errorMessage,
    isListReordering = () => false
}) {
    const dialog = document.querySelector("#card-dialog");
    const form = document.querySelector("#card-form");
    const dialogMainScroll = document.querySelector(".card-dialog-main-scroll");
    const sidebarContent = document.querySelector(".card-dialog-sidebar-content");
    const formMessage = document.querySelector("#card-form-message");
    const dialogTitle = document.querySelector("#card-dialog-title");
    const listIdInput = document.querySelector("#card-list-id");
    const cardIdInput = document.querySelector("#card-id");
    const titleInput = document.querySelector("#card-title");
    const descriptionInput = document.querySelector("#card-description");
    const dueDateInput = document.querySelector("#card-due-date");
    const dueDateButton = document.querySelector("#card-due-date-button");
    const dueDateDisplay = document.querySelector("#card-due-date-display");
    const datePicker = document.querySelector("#card-date-picker");
    const datePickerMonthLabel = document.querySelector("#card-calendar-month-label");
    const datePickerGrid = document.querySelector("#card-calendar-grid");
    const datePickerDayInput = document.querySelector("#card-due-date-day");
    const datePickerTimeInput = document.querySelector("#card-due-date-time");
    const datePickerError = document.querySelector("#card-date-picker-error");
    const clearDatePickerButton = document.querySelector("#clear-card-date-picker");
    const priorityInput = document.querySelector("#card-priority");
    const labelField = document.querySelector("#card-label-field");
    const selectedLabels = document.querySelector("#card-selected-labels");
    const selectedLabelsEmpty = document.querySelector("#card-selected-labels-empty");
    const labelPickerButton = document.querySelector("#card-label-picker-button");
    const labelPicker = document.querySelector("#card-label-picker");
    const labelPickerLoading = document.querySelector("#card-label-picker-loading");
    const labelPickerError = document.querySelector("#card-label-picker-error");
    const labelPickerEmpty = document.querySelector("#card-label-picker-empty");
    const labelOptions = document.querySelector("#card-label-options");
    const retryLabelsButton = document.querySelector("#retry-card-labels");
    const editActions = document.querySelector("#card-edit-actions");
    const dialogCompletionButton = document.querySelector("#card-dialog-completion-button");
    const deleteButton = document.querySelector("#delete-card-button");
    const saveButton = document.querySelector("#save-card-button");
    const commentManager = createCommentManager({ errorMessage });

    const cardsByListId = new Map();
    const loadingListIds = new Set();
    const listErrors = new Map();
    const busyCardIds = new Set();
    const busyLabelIds = new Set();
    const dropIndicator = document.createElement("div");
    const priorityLabels = {
        LOW: "Thấp",
        MEDIUM: "Trung bình",
        HIGH: "Cao",
        URGENT: "Khẩn cấp"
    };
    const longPressDelay = 360;
    const pressMoveTolerance = 10;
    const touchDragMoveThreshold = 4;
    const autoScrollEdge = 48;

    let activeCard = null;
    let dragState = null;
    let pressingCardId = null;
    let touchDragState = null;
    let touchAutoScrollFrame = null;
    let activeMouseCancel = null;
    let moveInProgress = false;
    let movingCardId = null;
    let suppressedOpenCardId = null;
    let suppressOpenUntil = 0;
    let calendarViewDate = new Date();
    let selectedPickerDate = "";
    let availableLabels = null;
    let labelsLoadError = null;
    let labelsLoadingPromise = null;

    dropIndicator.className = "card-drop-indicator";
    dropIndicator.setAttribute("aria-hidden", "true");

    function showFormMessage(text) {
        formMessage.textContent = text;
        formMessage.hidden = false;
    }

    function hideFormMessage() {
        formMessage.hidden = true;
        formMessage.textContent = "";
    }

    function closeDialog() {
        closeDatePicker(false);
        closeLabelPicker();
        commentManager.reset();
        activeCard = null;
        dialog.close();
    }

    function toDateTimeLocal(value) {
        return typeof value === "string" && value.length >= 16
            ? value.slice(0, 19)
            : "";
    }

    function fromDateTimeLocal(value) {
        return value.length === 16 ? `${value}:00` : value;
    }

    function padDatePart(value) {
        return String(value).padStart(2, "0");
    }

    function dateToInputValue(date) {
        return [
            date.getFullYear(),
            padDatePart(date.getMonth() + 1),
            padDatePart(date.getDate())
        ].join("-");
    }

    function parseInputDate(value) {
        const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value || "");
        if (!match) {
            return null;
        }
        const date = new Date(
            Number(match[1]),
            Number(match[2]) - 1,
            Number(match[3])
        );
        return date.getFullYear() === Number(match[1])
                && date.getMonth() === Number(match[2]) - 1
                && date.getDate() === Number(match[3])
            ? date
            : null;
    }

    function formatDateEntry(value) {
        const date = parseInputDate(value);
        return date
            ? `${padDatePart(date.getDate())}/${padDatePart(date.getMonth() + 1)}/${date.getFullYear()}`
            : "";
    }

    function parseDateEntry(value) {
        const normalized = (value || "").trim();
        if (parseInputDate(normalized)) {
            return normalized;
        }

        const match = /^(\d{1,2})\/(\d{1,2})\/(\d{4})$/.exec(normalized);
        if (!match) {
            return "";
        }
        const date = new Date(
            Number(match[3]),
            Number(match[2]) - 1,
            Number(match[1])
        );
        return date.getFullYear() === Number(match[3])
                && date.getMonth() === Number(match[2]) - 1
                && date.getDate() === Number(match[1])
            ? dateToInputValue(date)
            : "";
    }

    function splitDateTimeLocal(value) {
        const normalized = toDateTimeLocal(value);
        const [datePart = "", timePart = ""] = normalized.split("T");
        return {
            date: parseInputDate(datePart) ? datePart : "",
            time: /^\d{2}:\d{2}/.test(timePart) ? timePart.slice(0, 5) : ""
        };
    }

    function formatDueDateControl(value) {
        const parts = splitDateTimeLocal(value);
        const date = parseInputDate(parts.date);
        if (!date || !parts.time) {
            return "Chọn ngày và giờ";
        }

        const [hour, minute] = parts.time.split(":").map(Number);
        date.setHours(hour, minute, 0, 0);
        return date.toLocaleString("vi-VN", {
            day: "2-digit",
            month: "2-digit",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit"
        });
    }

    function updateDueDateControl() {
        const hasValue = Boolean(dueDateInput.value);
        dueDateDisplay.textContent = formatDueDateControl(dueDateInput.value);
        dueDateButton.classList.toggle("has-value", hasValue);
    }

    function setDatePickerError(text = "") {
        datePickerError.textContent = text;
        datePickerError.hidden = !text;
    }

    function renderDatePickerCalendar() {
        const viewYear = calendarViewDate.getFullYear();
        const viewMonth = calendarViewDate.getMonth();
        const selectedDate = selectedPickerDate;
        const today = dateToInputValue(new Date());
        const monthStart = new Date(viewYear, viewMonth, 1);
        const gridStart = new Date(viewYear, viewMonth, 1 - monthStart.getDay());

        datePickerMonthLabel.textContent = calendarViewDate.toLocaleDateString("vi-VN", {
            month: "long",
            year: "numeric"
        });
        datePickerGrid.replaceChildren();

        for (let index = 0; index < 42; index++) {
            const date = new Date(
                gridStart.getFullYear(),
                gridStart.getMonth(),
                gridStart.getDate() + index
            );
            const value = dateToInputValue(date);
            const dayButton = document.createElement("button");
            dayButton.type = "button";
            dayButton.className = "card-calendar-day";
            dayButton.textContent = String(date.getDate());
            dayButton.dataset.date = value;
            dayButton.setAttribute("role", "gridcell");
            dayButton.setAttribute("aria-selected", String(value === selectedDate));
            dayButton.setAttribute(
                "aria-label",
                date.toLocaleDateString("vi-VN", {
                    weekday: "long",
                    day: "numeric",
                    month: "long",
                    year: "numeric"
                })
            );
            dayButton.classList.toggle("is-outside-month", date.getMonth() !== viewMonth);
            dayButton.classList.toggle("is-today", value === today);
            dayButton.classList.toggle("is-selected", value === selectedDate);
            dayButton.addEventListener("click", () => {
                selectedPickerDate = value;
                datePickerDayInput.value = formatDateEntry(value);
                calendarViewDate = new Date(date.getFullYear(), date.getMonth(), 1);
                setDatePickerError();
                renderDatePickerCalendar();
                datePickerGrid.querySelector(".is-selected")?.focus();
            });
            datePickerGrid.append(dayButton);
        }
    }

    function positionDatePicker() {
        if (datePicker.hidden) {
            return;
        }

        const margin = 12;
        const gap = 8;
        const triggerBounds = dueDateButton.getBoundingClientRect();
        const pickerBounds = datePicker.getBoundingClientRect();
        const viewportWidth = window.innerWidth;
        const viewportHeight = window.innerHeight;
        const left = Math.min(
            Math.max(margin, triggerBounds.right - pickerBounds.width),
            Math.max(margin, viewportWidth - pickerBounds.width - margin)
        );

        let top = triggerBounds.bottom + gap;
        if (top + pickerBounds.height > viewportHeight - margin) {
            top = triggerBounds.top - pickerBounds.height - gap;
        }
        top = Math.min(
            Math.max(margin, top),
            Math.max(margin, viewportHeight - pickerBounds.height - margin)
        );

        datePicker.style.left = `${left}px`;
        datePicker.style.top = `${top}px`;
    }

    function closeDatePicker(returnFocus = true) {
        if (datePicker.hidden) {
            return;
        }
        datePicker.hidden = true;
        dueDateButton.setAttribute("aria-expanded", "false");
        setDatePickerError();
        if (returnFocus && dialog.open) {
            dueDateButton.focus();
        }
    }

    function openDatePicker() {
        const current = splitDateTimeLocal(dueDateInput.value);
        const now = new Date();
        const selectedDate = parseInputDate(current.date) || now;
        selectedPickerDate = current.date || dateToInputValue(selectedDate);
        datePickerDayInput.value = formatDateEntry(selectedPickerDate);
        datePickerTimeInput.value = current.time
            || `${padDatePart(now.getHours())}:${padDatePart(now.getMinutes())}`;
        clearDatePickerButton.hidden = !dueDateInput.value;
        calendarViewDate = new Date(
            selectedDate.getFullYear(),
            selectedDate.getMonth(),
            1
        );
        setDatePickerError();
        renderDatePickerCalendar();
        datePicker.hidden = false;
        dueDateButton.setAttribute("aria-expanded", "true");
        window.requestAnimationFrame(() => {
            positionDatePicker();
            const selectedDay = datePickerGrid.querySelector(".is-selected");
            (selectedDay || datePickerDayInput).focus();
        });
    }

    function applyDatePicker() {
        const dateValue = parseDateEntry(datePickerDayInput.value);
        const date = parseInputDate(dateValue);
        const time = /^\d{2}:\d{2}$/.test(datePickerTimeInput.value)
            ? datePickerTimeInput.value
            : "";
        if (!date || !time) {
            setDatePickerError("Vui lòng chọn đầy đủ ngày và giờ hết hạn.");
            return;
        }

        selectedPickerDate = dateValue;
        dueDateInput.value = `${dateValue}T${time}:00`;
        updateDueDateControl();
        hideFormMessage();
        closeDatePicker();
    }

    function clearDueDate() {
        dueDateInput.value = "";
        selectedPickerDate = "";
        updateDueDateControl();
        hideFormMessage();
        closeDatePicker();
    }

    function formatDateTime(value) {
        if (!value) {
            return "Chưa có hạn";
        }
        const date = new Date(value);
        return Number.isNaN(date.getTime())
            ? value
            : date.toLocaleString("vi-VN", {
                day: "2-digit",
                month: "2-digit",
                hour: "2-digit",
                minute: "2-digit"
            });
    }

    function isOverdue(card) {
        if (card.completed || !card.dueDate) {
            return false;
        }
        const dueDate = new Date(card.dueDate);
        return !Number.isNaN(dueDate.getTime()) && dueDate.getTime() < Date.now();
    }

    function replaceCard(updatedCard) {
        const cards = cardsByListId.get(updatedCard.listId) || [];
        cardsByListId.set(
            updatedCard.listId,
            cards.map((card) => card.id === updatedCard.id ? updatedCard : card)
        );
    }

    function isDraggingOrMoving() {
        return pressingCardId !== null || dragState !== null || moveInProgress;
    }

    function clearDropTargetVisuals() {
        dropIndicator.remove();
        document.querySelectorAll(".card-list-area.is-card-drop-target").forEach((element) => {
            element.classList.remove("is-card-drop-target");
        });
    }

    function stopTouchAutoScroll() {
        if (touchAutoScrollFrame !== null) {
            window.cancelAnimationFrame(touchAutoScrollFrame);
            touchAutoScrollFrame = null;
        }
    }

    function cleanupDragVisuals() {
        stopTouchAutoScroll();
        clearDropTargetVisuals();
        document.querySelectorAll(".task-card.is-card-dragging").forEach((element) => {
            element.classList.remove("is-card-dragging");
            element.removeAttribute("aria-grabbed");
        });
        document.querySelectorAll(".card-touch-drag-preview").forEach((element) => {
            element.remove();
        });
        document.querySelector("#board-list-track")?.classList.remove("is-card-reordering");
        pressingCardId = null;
    }

    function suppressCardOpen(cardId, duration = 800) {
        suppressedOpenCardId = cardId;
        suppressOpenUntil = performance.now() + duration;
    }

    function shouldSuppressCardOpen(cardId) {
        if (suppressedOpenCardId !== cardId) {
            return false;
        }
        if (performance.now() < suppressOpenUntil) {
            return true;
        }
        suppressedOpenCardId = null;
        suppressOpenUntil = 0;
        return false;
    }

    function clearCardOpenSuppression(cardId) {
        if (suppressedOpenCardId === cardId) {
            suppressedOpenCardId = null;
            suppressOpenUntil = 0;
        }
    }

    function isInteractiveCardTarget(target) {
        return target instanceof Element
            && Boolean(target.closest("button, a, input, textarea, select, [contenteditable='true']"));
    }

    function activateCardDrag(card, boardList, cardElement) {
        if (moveInProgress || busyCardIds.has(card.id) || isListReordering()) {
            return false;
        }

        const sourceCards = cardsByListId.get(boardList.id) || [];
        const sourceIndex = sourceCards.findIndex((item) => item.id === card.id);
        if (sourceIndex < 0) {
            return false;
        }

        dragState = {
            cardId: card.id,
            cardTitle: card.title,
            sourceListId: boardList.id,
            sourcePosition: sourceIndex
        };
        cardElement.classList.add("is-card-dragging");
        cardElement.setAttribute("aria-grabbed", "true");
        document.querySelector("#board-list-track")?.classList.add("is-card-reordering");
        return true;
    }

    function beginCardDrag(event, card, boardList, cardElement) {
        if (!activateCardDrag(card, boardList, cardElement)) {
            event.preventDefault();
            return false;
        }

        event.stopPropagation();
        event.dataTransfer.effectAllowed = "move";
        event.dataTransfer.setData("text/plain", String(card.id));
        event.dataTransfer.setDragImage(cardElement, 18, 18);
        return true;
    }

    function endCardDrag(event, cardId) {
        event.stopPropagation();
        suppressCardOpen(cardId);
        cleanupDragVisuals();
        dragState = null;
    }

    function canDropInList(listId) {
        return Number.isInteger(listId)
            && cardsByListId.has(listId)
            && !loadingListIds.has(listId)
            && !listErrors.has(listId);
    }

    function positionDropIndicator(clientY, container) {
        if (!dragState || moveInProgress) {
            return null;
        }

        const cardStack = container.querySelector(".task-card-stack");
        if (!cardStack) {
            return null;
        }

        const otherCards = [...cardStack.children].filter((element) =>
            element.classList.contains("task-card")
                && Number(element.dataset.cardId) !== dragState.cardId
        );
        const insertBefore = otherCards.find((element) => {
            const bounds = element.getBoundingClientRect();
            return clientY < bounds.top + bounds.height / 2;
        });

        clearDropTargetVisuals();
        container.classList.add("is-card-drop-target");

        if (insertBefore) {
            cardStack.insertBefore(dropIndicator, insertBefore);
        } else {
            cardStack.append(dropIndicator);
        }
        return cardStack;
    }

    function targetPositionFromIndicator(cardStack) {
        let targetPosition = 0;
        for (const element of cardStack.children) {
            if (element === dropIndicator) {
                break;
            }
            if (element.classList.contains("task-card")
                    && Number(element.dataset.cardId) !== dragState.cardId) {
                targetPosition++;
            }
        }
        return targetPosition;
    }

    function findTouch(touchList, identifier) {
        return Array.from(touchList)
            .find((touch) => touch.identifier === identifier) || null;
    }

    function findDropContainerAt(clientX, clientY) {
        const element = document.elementFromPoint(clientX, clientY);
        const container = element?.closest(".card-list-area");
        if (!container) {
            return null;
        }

        const listId = Number(container.dataset.listId);
        return canDropInList(listId) ? container : null;
    }

    function createTouchDragPreview(cardElement) {
        const bounds = cardElement.getBoundingClientRect();
        const preview = cardElement.cloneNode(true);
        preview.classList.remove("is-card-dragging");
        preview.classList.add("card-touch-drag-preview");
        preview.removeAttribute("tabindex");
        preview.removeAttribute("aria-grabbed");
        preview.setAttribute("aria-hidden", "true");
        preview.draggable = false;
        preview.style.width = `${bounds.width}px`;
        preview.querySelectorAll("button").forEach((button) => {
            button.tabIndex = -1;
        });
        document.body.append(preview);
        return preview;
    }

    function updateTouchDragPreview(clientX, clientY) {
        if (!touchDragState?.preview) {
            return;
        }
        touchDragState.preview.style.transform =
            `translate3d(${clientX + 14}px, ${clientY + 14}px, 0)`;
    }

    function updateTouchDropTarget(clientX, clientY) {
        if (!touchDragState?.active || !dragState) {
            return null;
        }

        const container = findDropContainerAt(clientX, clientY);
        if (!container) {
            touchDragState.targetListId = null;
            clearDropTargetVisuals();
            return null;
        }

        const cardStack = positionDropIndicator(clientY, container);
        touchDragState.targetListId = Number(container.dataset.listId);
        return cardStack;
    }

    function scrollElementNearEdge(element, coordinate, start, end, axis) {
        let delta = 0;
        if (coordinate < start + autoScrollEdge) {
            delta = -12;
        } else if (coordinate > end - autoScrollEdge) {
            delta = 12;
        }
        if (delta === 0) {
            return false;
        }

        const before = axis === "x" ? element.scrollLeft : element.scrollTop;
        if (axis === "x") {
            element.scrollLeft += delta;
            return element.scrollLeft !== before;
        }
        element.scrollTop += delta;
        return element.scrollTop !== before;
    }

    function performTouchAutoScroll() {
        touchAutoScrollFrame = null;
        if (!touchDragState?.active) {
            return;
        }

        const { lastX, lastY } = touchDragState;
        let scrolled = false;
        const listScroller = document.querySelector("#board-list-scroll");
        if (listScroller) {
            const bounds = listScroller.getBoundingClientRect();
            scrolled = scrollElementNearEdge(
                listScroller,
                lastX,
                bounds.left,
                bounds.right,
                "x"
            ) || scrolled;
        }

        const targetContainer = findDropContainerAt(lastX, lastY);
        if (targetContainer) {
            const bounds = targetContainer.getBoundingClientRect();
            scrolled = scrollElementNearEdge(
                targetContainer,
                lastY,
                bounds.top,
                bounds.bottom,
                "y"
            ) || scrolled;
        }

        if (scrolled) {
            touchDragState.hasMoved = true;
            updateTouchDropTarget(lastX, lastY);
            touchAutoScrollFrame = window.requestAnimationFrame(performTouchAutoScroll);
        }
    }

    function scheduleTouchAutoScroll() {
        if (touchAutoScrollFrame === null && touchDragState?.active) {
            touchAutoScrollFrame = window.requestAnimationFrame(performTouchAutoScroll);
        }
    }

    function restoreTouchDraggable(state) {
        if (state.cardElement?.isConnected) {
            state.cardElement.draggable = state.cardWasDraggable;
        }
        if (state.boardListElement?.isConnected) {
            state.boardListElement.draggable = state.boardListWasDraggable;
        }
    }

    function removePendingTouchScrollListeners(state) {
        if (!state?.cancelForScroll) {
            return;
        }
        state.sourceContainer?.removeEventListener("scroll", state.cancelForScroll);
        state.listScroller?.removeEventListener("scroll", state.cancelForScroll);
        window.removeEventListener("scroll", state.cancelForScroll);
    }

    function cancelPendingTouchForScroll(state) {
        if (touchDragState !== state || state.active || state.cancelled) {
            return;
        }
        if (state.holdTimer !== null) {
            window.clearTimeout(state.holdTimer);
            state.holdTimer = null;
        }
        state.cancelled = true;
        removePendingTouchScrollListeners(state);
        suppressCardOpen(state.cardId, 500);
        if (pressingCardId === state.cardId) {
            pressingCardId = null;
        }
    }

    function cancelActiveCardInteraction() {
        if (activeMouseCancel) {
            activeMouseCancel();
            activeMouseCancel = null;
        }

        const state = touchDragState;
        if (state) {
            if (state.holdTimer !== null) {
                window.clearTimeout(state.holdTimer);
                state.holdTimer = null;
            }
            removePendingTouchScrollListeners(state);
            restoreTouchDraggable(state);
        }

        touchDragState = null;
        dragState = null;
        cleanupDragVisuals();
    }

    async function synchronizeCardLists(listIds, exposeErrors) {
        const uniqueListIds = [...new Set(listIds)];
        try {
            const refreshedLists = await Promise.all(uniqueListIds.map(async (listId) => ({
                listId,
                cards: await api.get(`/api/lists/${listId}/cards`)
            })));

            for (const refreshedList of refreshedLists) {
                cardsByListId.set(refreshedList.listId, refreshedList.cards);
                listErrors.delete(refreshedList.listId);
            }
            return [];
        } catch (error) {
            if (exposeErrors) {
                for (const listId of uniqueListIds) {
                    listErrors.set(listId, error);
                }
            }
            if (!(error instanceof ApiError)) {
                console.error(error);
            }
            return [error];
        }
    }

    async function moveCard(cardDrag, targetListId, targetPosition) {
        if (moveInProgress) {
            return;
        }

        const affectedListIds = [cardDrag.sourceListId, targetListId];
        moveInProgress = true;
        movingCardId = cardDrag.cardId;
        onChange();

        try {
            await api.patch(`/api/cards/${cardDrag.cardId}/move`, {
                targetListId,
                targetPosition
            });
            const syncFailures = await synchronizeCardLists(affectedListIds, true);
            if (syncFailures.length !== 0) {
                showErrorToast(
                    "Không thể đồng bộ giao diện sau khi di chuyển thẻ. Vui lòng tải lại trang."
                );
            }
        } catch (error) {
            const syncFailures = await synchronizeCardLists(affectedListIds, false);
            const syncHint = syncFailures.length === 0
                ? ""
                : " Giao diện chưa thể đồng bộ lại đầy đủ; hãy tải lại trang.";
            showErrorToast(
                `${errorMessage(
                    error,
                    "Không thể di chuyển thẻ. Vui lòng thử lại."
                )}${syncHint}`
            );
            if (!(error instanceof ApiError)) {
                console.error(error);
            }
        } finally {
            moveInProgress = false;
            movingCardId = null;
            onChange();
        }
    }

    async function completeCardDrop(targetListId, cardStack) {
        if (!dragState || moveInProgress || !cardStack) {
            return;
        }

        const cardDrag = dragState;
        const targetPosition = targetPositionFromIndicator(cardStack);
        suppressCardOpen(cardDrag.cardId);
        cleanupDragVisuals();
        dragState = null;

        if (cardDrag.sourceListId === targetListId
                && cardDrag.sourcePosition === targetPosition) {
            return;
        }
        await moveCard(cardDrag, targetListId, targetPosition);
    }

    async function dropCard(event, targetListId, container) {
        if (!dragState || moveInProgress || !canDropInList(targetListId)) {
            return;
        }

        event.preventDefault();
        event.stopPropagation();
        const cardStack = positionDropIndicator(event.clientY, container);
        await completeCardDrop(targetListId, cardStack);
    }

    function createButton(text, className, action, disabled = false) {
        const button = document.createElement("button");
        button.type = "button";
        button.className = className;
        button.textContent = text;
        button.disabled = disabled;
        button.addEventListener("click", action);
        return button;
    }

    function syncDialogCompletion(card) {
        dialogCompletionButton.textContent = card.completed
            ? "Mở lại Card"
            : "Đánh dấu hoàn thành";
        dialogCompletionButton.classList.toggle("is-completed", card.completed);
    }

    function sortLabels(labels) {
        return [...(labels || [])].sort((left, right) =>
            left.name.localeCompare(right.name, "vi")
        );
    }

    function closeLabelPicker() {
        labelPicker.hidden = true;
        labelPickerButton.setAttribute("aria-expanded", "false");
    }

    function renderSelectedLabels() {
        const labels = sortLabels(activeCard?.labels);
        selectedLabels.replaceChildren(
            ...labels.map((label) => createLabelChip(label, "card-dialog-label-chip"))
        );
        selectedLabelsEmpty.hidden = labels.length > 0;
    }

    function renderLabelPicker() {
        labelPickerLoading.hidden = labelsLoadingPromise === null;
        labelPickerError.hidden = labelsLoadError === null;
        labelPickerEmpty.hidden = availableLabels === null || availableLabels.length > 0;
        labelOptions.replaceChildren();

        if (labelsLoadError) {
            labelPickerError.querySelector("p").textContent = errorMessage(
                labelsLoadError,
                "Không thể tải danh sách nhãn."
            );
            return;
        }
        if (!availableLabels) {
            return;
        }

        const attachedIds = new Set((activeCard?.labels || []).map((label) => label.id));
        for (const label of availableLabels) {
            const option = document.createElement("label");
            option.className = "card-label-option";

            const checkbox = document.createElement("input");
            checkbox.type = "checkbox";
            checkbox.checked = attachedIds.has(label.id);
            checkbox.disabled = busyLabelIds.has(label.id);
            checkbox.setAttribute("aria-label", `${checkbox.checked ? "Gỡ" : "Gắn"} nhãn ${label.name}`);
            checkbox.addEventListener("change", () => {
                void updateCardLabel(label, checkbox.checked);
            });

            option.append(checkbox, createLabelChip(label, "card-label-option-chip"));
            labelOptions.append(option);
        }
    }

    async function loadAvailableLabels(force = false) {
        if (labelsLoadingPromise || (!force && availableLabels !== null)) {
            return labelsLoadingPromise;
        }
        labelsLoadError = null;
        if (force) {
            availableLabels = null;
        }
        labelsLoadingPromise = api.get("/api/labels");
        renderLabelPicker();
        try {
            availableLabels = sortLabels(await labelsLoadingPromise);
        } catch (error) {
            labelsLoadError = error;
            if (!(error instanceof ApiError)) {
                console.error(error);
            }
        } finally {
            labelsLoadingPromise = null;
            renderLabelPicker();
        }
    }

    async function updateCardLabel(label, shouldAttach) {
        if (!activeCard || busyLabelIds.has(label.id)) {
            return;
        }

        const cardId = activeCard.id;
        busyLabelIds.add(label.id);
        renderLabelPicker();
        try {
            const url = `/api/cards/${cardId}/labels/${label.id}`;
            if (shouldAttach) {
                await api.post(url);
            } else {
                await api.delete(url);
            }

            const currentLabels = activeCard.labels || [];
            const nextLabels = shouldAttach
                ? sortLabels([
                    ...currentLabels.filter((item) => item.id !== label.id),
                    label
                ])
                : currentLabels.filter((item) => item.id !== label.id);
            activeCard = { ...activeCard, labels: nextLabels };
            replaceCard(activeCard);
            renderSelectedLabels();
            showSuccessToast(
                shouldAttach
                    ? `Đã gắn nhãn "${label.name}" vào Card.`
                    : `Đã gỡ nhãn "${label.name}" khỏi Card.`
            );
            onChange();
        } catch (error) {
            showErrorToast(errorMessage(
                error,
                shouldAttach ? "Không thể gắn nhãn vào Card." : "Không thể gỡ nhãn khỏi Card."
            ));
            if (!(error instanceof ApiError)) {
                console.error(error);
            }
        } finally {
            busyLabelIds.delete(label.id);
            renderLabelPicker();
        }
    }

    function createCardElement(card, boardList) {
        const cardElement = document.createElement("article");
        cardElement.className = "task-card";
        cardElement.dataset.cardId = String(card.id);
        cardElement.dataset.sourceListId = String(boardList.id);
        cardElement.dataset.sourcePosition = String(card.position);
        const cardBusy = moveInProgress || busyCardIds.has(card.id);
        cardElement.draggable = !cardBusy;
        cardElement.tabIndex = cardBusy ? -1 : 0;
        cardElement.title = "Nhấn để xem chi tiết; nhấn giữ rồi kéo để di chuyển";
        cardElement.setAttribute("aria-haspopup", "dialog");
        cardElement.setAttribute(
            "aria-label",
            `${card.title}. Nhấn để xem chi tiết; nhấn giữ rồi kéo để di chuyển.`
        );
        if (cardBusy) {
            cardElement.setAttribute("aria-disabled", "true");
        }
        if (movingCardId === card.id) {
            cardElement.classList.add("is-card-move-pending");
        }
        if (card.completed) {
            cardElement.classList.add("is-completed");
        } else if (isOverdue(card)) {
            cardElement.classList.add("is-overdue");
        }

        const topLine = document.createElement("div");
        topLine.className = "task-card-topline";

        const priority = document.createElement("span");
        priority.className = `card-priority priority-${card.priority.toLowerCase()}`;
        priority.textContent = priorityLabels[card.priority] || card.priority;
        priority.title = `Độ ưu tiên: ${priority.textContent}`;

        for (const label of sortLabels(card.labels)) {
            topLine.append(createLabelChip(label, "task-card-label"));
        }
        topLine.append(priority);
        cardElement.append(topLine);

        let dragArmed = false;
        let mousePointerId = null;

        function cancelMouseArm() {
            dragArmed = false;
            clearMouseArm();
        }

        function clearMouseArm(keepArmed = false) {
            window.removeEventListener("pointerup", finishMouseArm);
            window.removeEventListener("pointercancel", finishMouseArm);
            mousePointerId = null;
            if (!keepArmed) {
                dragArmed = false;
                if (pressingCardId === card.id && dragState?.cardId !== card.id) {
                    pressingCardId = null;
                }
                if (activeMouseCancel === cancelMouseArm) {
                    activeMouseCancel = null;
                }
            }
        }

        function finishMouseArm(event) {
            if (mousePointerId === null || event.pointerId !== mousePointerId) {
                return;
            }
            clearMouseArm();
        }

        cardElement.addEventListener("pointerdown", (event) => {
            if (event.pointerType !== "mouse" || event.button !== 0 || cardBusy
                    || isInteractiveCardTarget(event.target) || isListReordering()
                    || pressingCardId !== null) {
                return;
            }

            clearCardOpenSuppression(card.id);
            pressingCardId = card.id;
            mousePointerId = event.pointerId;
            dragArmed = true;
            activeMouseCancel = cancelMouseArm;
            window.addEventListener("pointerup", finishMouseArm);
            window.addEventListener("pointercancel", finishMouseArm);
        });
        cardElement.addEventListener("dragstart", (event) => {
            if (!dragArmed) {
                event.preventDefault();
                event.stopPropagation();
                clearMouseArm();
                return;
            }
            clearMouseArm(true);
            if (!beginCardDrag(event, card, boardList, cardElement)) {
                dragArmed = false;
                cleanupDragVisuals();
            }
        });
        cardElement.addEventListener("dragend", (event) => {
            cancelMouseArm();
            endCardDrag(event, card.id);
        });

        const heading = document.createElement("div");
        heading.className = "task-card-heading";

        const completionButton = createButton(
            card.completed ? "✓" : "",
            "card-completion-toggle",
            (event) => {
                event.stopPropagation();
                updateCompletion(card);
            },
            cardBusy
        );
        completionButton.title = card.completed ? "Mở lại Card" : "Đánh dấu hoàn thành";
        completionButton.setAttribute(
            "aria-label",
            card.completed
                ? `Mở lại Card ${card.title}`
                : `Đánh dấu hoàn thành Card ${card.title}`
        );
        completionButton.setAttribute("aria-pressed", String(card.completed));

        const title = document.createElement("h4");
        title.className = "task-card-title";
        title.textContent = card.title;

        heading.append(completionButton, title);
        cardElement.append(heading);

        const metadata = document.createElement("div");
        metadata.className = "task-card-metadata";

        if (card.description) {
            const descriptionIndicator = document.createElement("span");
            descriptionIndicator.className = "card-description-indicator";
            descriptionIndicator.textContent = "≡";
            descriptionIndicator.title = "Card có mô tả";
            descriptionIndicator.setAttribute("aria-label", "Card có mô tả");
            metadata.append(descriptionIndicator);
        }

        if (card.dueDate) {
            const dueDate = document.createElement("span");
            dueDate.className = "task-card-due-date";
            if (card.completed) {
                dueDate.classList.add("is-completed");
                dueDate.textContent = `✓ ${formatDateTime(card.dueDate)}`;
                dueDate.title = "Đã hoàn thành";
            } else if (isOverdue(card)) {
                dueDate.classList.add("is-overdue");
                dueDate.textContent = `◷ ${formatDateTime(card.dueDate)}`;
                dueDate.title = "Đã quá hạn";
            } else {
                dueDate.textContent = `◷ ${formatDateTime(card.dueDate)}`;
                dueDate.title = "Hạn hoàn thành";
            }
            metadata.append(dueDate);
        }
        if (metadata.childElementCount > 0) {
            cardElement.append(metadata);
        }

        function openCardDetails() {
            if (!cardBusy && !moveInProgress && !isListReordering()
                    && !shouldSuppressCardOpen(card.id)) {
                openEditDialog(card);
            }
        }

        cardElement.addEventListener("click", (event) => {
            if (isInteractiveCardTarget(event.target)) {
                return;
            }
            openCardDetails();
        });
        cardElement.addEventListener("keydown", (event) => {
            if (event.target !== cardElement || (event.key !== "Enter" && event.key !== " ")) {
                return;
            }
            event.preventDefault();
            openCardDetails();
        });

        cardElement.addEventListener("touchstart", (event) => {
            if (event.touches.length !== 1 || cardBusy || isListReordering()
                    || isInteractiveCardTarget(event.target) || touchDragState
                    || pressingCardId !== null) {
                return;
            }

            clearCardOpenSuppression(card.id);
            const touch = event.changedTouches[0];
            const boardListElement = cardElement.closest(".board-list-card");
            const sourceContainer = cardElement.closest(".card-list-area");
            const listScroller = document.querySelector("#board-list-scroll");
            pressingCardId = card.id;
            touchDragState = {
                active: false,
                cancelled: false,
                hasMoved: false,
                cardId: card.id,
                identifier: touch.identifier,
                startX: touch.clientX,
                startY: touch.clientY,
                lastX: touch.clientX,
                lastY: touch.clientY,
                cardElement,
                cardWasDraggable: cardElement.draggable,
                boardListElement,
                boardListWasDraggable: boardListElement?.draggable ?? false,
                sourceContainer,
                sourceScrollTop: sourceContainer?.scrollTop ?? 0,
                listScroller,
                listScrollLeft: listScroller?.scrollLeft ?? 0,
                pageScrollX: window.scrollX,
                pageScrollY: window.scrollY,
                cancelForScroll: null,
                preview: null,
                targetListId: null,
                holdTimer: null
            };
            cardElement.draggable = false;
            if (boardListElement) {
                boardListElement.draggable = false;
            }

            const state = touchDragState;
            state.cancelForScroll = () => {
                cancelPendingTouchForScroll(state);
            };
            state.sourceContainer?.addEventListener("scroll", state.cancelForScroll, {
                passive: true
            });
            state.listScroller?.addEventListener("scroll", state.cancelForScroll, {
                passive: true
            });
            window.addEventListener("scroll", state.cancelForScroll, { passive: true });
            state.holdTimer = window.setTimeout(() => {
                if (touchDragState !== state || state.cancelled) {
                    return;
                }
                state.holdTimer = null;
                const scrolledBeforeActivation =
                    (state.sourceContainer
                        && state.sourceContainer.scrollTop !== state.sourceScrollTop)
                    || (state.listScroller
                        && state.listScroller.scrollLeft !== state.listScrollLeft)
                    || window.scrollX !== state.pageScrollX
                    || window.scrollY !== state.pageScrollY;
                if (scrolledBeforeActivation) {
                    cancelPendingTouchForScroll(state);
                    return;
                }
                removePendingTouchScrollListeners(state);
                if (!activateCardDrag(card, boardList, cardElement)) {
                    state.cancelled = true;
                    suppressCardOpen(card.id);
                    return;
                }

                state.active = true;
                state.preview = createTouchDragPreview(cardElement);
                suppressCardOpen(card.id);
                updateTouchDragPreview(state.lastX, state.lastY);
                updateTouchDropTarget(state.lastX, state.lastY);
            }, longPressDelay);
        }, { passive: true });

        cardElement.addEventListener("touchmove", (event) => {
            const state = touchDragState;
            if (!state || state.cardId !== card.id || state.cancelled) {
                return;
            }

            const touch = findTouch(event.touches, state.identifier);
            if (!touch) {
                return;
            }
            state.lastX = touch.clientX;
            state.lastY = touch.clientY;
            const distance = Math.hypot(
                state.lastX - state.startX,
                state.lastY - state.startY
            );

            if (!state.active) {
                if (distance > pressMoveTolerance) {
                    cancelPendingTouchForScroll(state);
                }
                return;
            }

            event.preventDefault();
            event.stopPropagation();
            state.hasMoved = state.hasMoved || distance > touchDragMoveThreshold;
            updateTouchDragPreview(state.lastX, state.lastY);
            updateTouchDropTarget(state.lastX, state.lastY);
            if (state.hasMoved) {
                scheduleTouchAutoScroll();
            }
        }, { passive: false });

        function finishTouchGesture(event, cancelled) {
            const state = touchDragState;
            if (!state || state.cardId !== card.id) {
                return;
            }

            const touch = findTouch(event.changedTouches, state.identifier);
            if (!touch && event.type !== "touchcancel") {
                return;
            }
            if (touch) {
                state.lastX = touch.clientX;
                state.lastY = touch.clientY;
            }
            if (state.holdTimer !== null) {
                window.clearTimeout(state.holdTimer);
                state.holdTimer = null;
            }
            removePendingTouchScrollListeners(state);
            restoreTouchDraggable(state);

            if (!state.active) {
                touchDragState = null;
                if (pressingCardId === card.id) {
                    pressingCardId = null;
                }
                return;
            }

            event.preventDefault();
            event.stopPropagation();
            suppressCardOpen(card.id);

            let targetListId = null;
            let cardStack = null;
            if (!cancelled && state.hasMoved) {
                const container = findDropContainerAt(state.lastX, state.lastY);
                if (container) {
                    targetListId = Number(container.dataset.listId);
                    cardStack = positionDropIndicator(state.lastY, container);
                }
            }

            touchDragState = null;
            if (targetListId !== null && cardStack) {
                void completeCardDrop(targetListId, cardStack);
                return;
            }

            cleanupDragVisuals();
            dragState = null;
        }

        cardElement.addEventListener("touchend", (event) => {
            finishTouchGesture(event, false);
        }, { passive: false });
        cardElement.addEventListener("touchcancel", (event) => {
            finishTouchGesture(event, true);
        }, { passive: false });
        cardElement.addEventListener("contextmenu", (event) => {
            if (dragArmed || (touchDragState?.cardId === card.id && touchDragState.active)) {
                event.preventDefault();
            }
        });
        return cardElement;
    }

    function renderBoardList(boardList, container) {
        container.replaceChildren();
        container.classList.add("card-list-area");
        container.classList.remove("is-empty");
        container.dataset.listId = String(boardList.id);

        const cards = cardsByListId.get(boardList.id);
        const addButton = createButton(
            "＋ Thêm thẻ",
            "card-add-button",
            () => openCreateDialog(boardList.id),
            moveInProgress
        );

        if (loadingListIds.has(boardList.id)) {
            const loading = document.createElement("p");
            loading.className = "card-list-state";
            loading.textContent = "Đang tải Card...";
            container.append(loading, addButton);
            return;
        }

        if (listErrors.has(boardList.id)) {
            const error = document.createElement("div");
            error.className = "card-list-state card-list-error";

            const text = document.createElement("p");
            text.textContent = errorMessage(listErrors.get(boardList.id), "Không thể tải Card.");

            const retry = createButton(
                "Thử lại",
                "card-action",
                () => refreshList(boardList.id)
            );
            error.append(text, retry);
            container.append(error, addButton);
            return;
        }

        if (!cardsByListId.has(boardList.id)) {
            const loading = document.createElement("p");
            loading.className = "card-list-state";
            loading.textContent = "Đang tải Card...";
            container.append(loading, addButton);
            return;
        }

        const cardStack = document.createElement("div");
        cardStack.className = "task-card-stack";
        cardStack.dataset.listId = String(boardList.id);

        const isEmpty = cards.length === 0;
        container.classList.toggle("is-empty", isEmpty);
        cardStack.classList.toggle("is-empty", isEmpty);

        if (!isEmpty) {
            for (const card of cards) {
                cardStack.append(createCardElement(card, boardList));
            }
        }

//        if (cards.length === 0) {
//            const empty = document.createElement("p");
//            empty.className = "card-list-state card-list-empty-state";
//            empty.textContent = "Chưa có thẻ công việc";
//            cardStack.append(empty);
//        } else {
//            for (const card of cards) {
//                cardStack.append(createCardElement(card, boardList));
//            }
//        }

        container.append(cardStack, addButton);
        container.addEventListener("dragover", (event) => {
            if (!dragState || moveInProgress || isListReordering()) {
                return;
            }
            event.preventDefault();
            event.stopPropagation();
            event.dataTransfer.dropEffect = "move";
            positionDropIndicator(event.clientY, container);
        });
        container.addEventListener("dragleave", (event) => {
            if (!event.relatedTarget || !container.contains(event.relatedTarget)) {
                clearDropTargetVisuals();
            }
        });
        container.addEventListener("drop", (event) => {
            dropCard(event, boardList.id, container);
        });
    }

    function openCreateDialog(listId) {
        closeDatePicker(false);
        closeLabelPicker();
        commentManager.reset();
        form.reset();
        activeCard = null;
        listIdInput.value = String(listId);
        cardIdInput.value = "";
        dueDateInput.value = "";
        updateDueDateControl();
        priorityInput.value = "MEDIUM";
        dialogTitle.textContent = "Tạo Card";
        editActions.hidden = true;
        labelField.hidden = true;
        saveButton.textContent = "Tạo Card";
        hideFormMessage();
        dialog.showModal();
        resetDialogScroll();
    }

    function resetDialogScroll() {
        dialogMainScroll.scrollTop = 0;
        sidebarContent.scrollTop = 0;
        window.requestAnimationFrame(() => {
            dialogMainScroll.scrollTop = 0;
            sidebarContent.scrollTop = 0;
            titleInput.focus({ preventScroll: true });
        });
    }

    function openEditDialog(card) {
        closeDatePicker(false);
        closeLabelPicker();
        commentManager.reset();
        form.reset();
        activeCard = card;
        listIdInput.value = String(card.listId);
        cardIdInput.value = String(card.id);
        titleInput.value = card.title;
        descriptionInput.value = card.description || "";
        dueDateInput.value = toDateTimeLocal(card.dueDate);
        updateDueDateControl();
        priorityInput.value = card.priority;
        dialogTitle.textContent = card.title;
        editActions.hidden = false;
        labelField.hidden = false;
        renderSelectedLabels();
        renderLabelPicker();
        void loadAvailableLabels();
        syncDialogCompletion(card);
        saveButton.textContent = "Lưu thay đổi";
        hideFormMessage();
        dialog.showModal();
        void commentManager.openCard(card.id);
        resetDialogScroll();
    }

    async function saveCard(event) {
        event.preventDefault();
        if (!form.reportValidity()) {
            return;
        }

        const listId = Number(listIdInput.value);
        const cardId = cardIdInput.value;
        const body = {
            title: titleInput.value,
            description: descriptionInput.value,
            dueDate: dueDateInput.value
                ? fromDateTimeLocal(dueDateInput.value)
                : null,
            priority: priorityInput.value
        };
        saveButton.disabled = true;

        try {
            if (cardId) {
                const updated = await api.put(`/api/cards/${cardId}`, body);
                replaceCard(updated);
                showSuccessToast("Cập nhật thẻ công việc thành công.");
            } else {
                const created = await api.post(`/api/lists/${listId}/cards`, body);
                const cards = cardsByListId.get(listId) || [];
                cardsByListId.set(
                    listId,
                    [...cards, created].sort((left, right) => left.position - right.position)
                );
                showSuccessToast("Tạo thẻ công việc thành công.");
            }
            closeDialog();
            onChange();
        } catch (error) {
            showErrorToast(errorMessage(
                error,
                cardId
                    ? "Không thể cập nhật thẻ công việc."
                    : "Không thể tạo thẻ công việc."
            ));
            if (!(error instanceof ApiError)) {
                console.error(error);
            }
        } finally {
            saveButton.disabled = false;
        }
    }

    async function updateCompletion(card) {
        if (busyCardIds.has(card.id)) {
            return null;
        }
        busyCardIds.add(card.id);
        onChange();

        try {
            const updated = await api.patch(`/api/cards/${card.id}/completion`, {
                completed: !card.completed
            });
            replaceCard(updated);
            if (activeCard?.id === updated.id) {
                activeCard = updated;
                syncDialogCompletion(updated);
            }
            showSuccessToast(
                updated.completed
                    ? "Đã đánh dấu thẻ là hoàn thành."
                    : "Đã mở lại thẻ công việc."
            );
            return updated;
        } catch (error) {
            showErrorToast(errorMessage(error, "Không thể cập nhật trạng thái thẻ công việc."));
            if (!(error instanceof ApiError)) {
                console.error(error);
            }
            return null;
        } finally {
            busyCardIds.delete(card.id);
            onChange();
        }
    }

    async function deleteCard(card) {
        if (busyCardIds.has(card.id)
                || !window.confirm(`Xóa Card "${card.title}"? Thao tác này không thể khôi phục.`)) {
            return;
        }

        busyCardIds.add(card.id);
        onChange();
        try {
            await api.delete(`/api/cards/${card.id}`);
            const cards = await api.get(`/api/lists/${card.listId}/cards`);
            cardsByListId.set(card.listId, cards);
            if (activeCard?.id === card.id) {
                closeDialog();
            }
            showSuccessToast("Xóa thẻ công việc thành công.");
        } catch (error) {
            showErrorToast(errorMessage(error, "Không thể xóa thẻ công việc."));
            if (!(error instanceof ApiError)) {
                console.error(error);
            }
        } finally {
            busyCardIds.delete(card.id);
            onChange();
        }
    }

    async function refreshList(listId) {
        loadingListIds.add(listId);
        listErrors.delete(listId);
        onChange();
        try {
            cardsByListId.set(listId, await api.get(`/api/lists/${listId}/cards`));
        } catch (error) {
            listErrors.set(listId, error);
            if (!(error instanceof ApiError)) {
                console.error(error);
            }
        } finally {
            loadingListIds.delete(listId);
            onChange();
        }
    }

    async function loadCards(boardLists) {
        const activeListIds = new Set(boardLists.map((boardList) => boardList.id));
        for (const listId of cardsByListId.keys()) {
            if (!activeListIds.has(listId)) {
                cardsByListId.delete(listId);
                listErrors.delete(listId);
            }
        }

        await Promise.all(boardLists.map(async (boardList) => {
            loadingListIds.add(boardList.id);
            listErrors.delete(boardList.id);
            try {
                cardsByListId.set(
                    boardList.id,
                    await api.get(`/api/lists/${boardList.id}/cards`)
                );
            } catch (error) {
                listErrors.set(boardList.id, error);
                if (!(error instanceof ApiError)) {
                    console.error(error);
                }
            } finally {
                loadingListIds.delete(boardList.id);
            }
        }));
    }

    function initializeList(listId) {
        cardsByListId.set(listId, []);
        listErrors.delete(listId);
    }

    function removeList(listId) {
        cardsByListId.delete(listId);
        loadingListIds.delete(listId);
        listErrors.delete(listId);
    }

    document.querySelector("#close-card-dialog").addEventListener("click", closeDialog);
    document.querySelector("#cancel-card-button").addEventListener("click", closeDialog);
    dueDateButton.addEventListener("click", () => {
        if (datePicker.hidden) {
            openDatePicker();
        } else {
            closeDatePicker();
        }
    });
    labelPickerButton.addEventListener("click", () => {
        const willOpen = labelPicker.hidden;
        labelPicker.hidden = !willOpen;
        labelPickerButton.setAttribute("aria-expanded", String(willOpen));
        if (willOpen) {
            renderLabelPicker();
            void loadAvailableLabels();
        }
    });
    retryLabelsButton.addEventListener("click", () => {
        void loadAvailableLabels(true);
    });
    document.querySelector("#close-card-date-picker").addEventListener("click", () => {
        closeDatePicker();
    });
    document.querySelector("#cancel-card-date-picker").addEventListener("click", () => {
        closeDatePicker();
    });
    clearDatePickerButton.addEventListener("click", clearDueDate);
    document.querySelector("#apply-card-date-picker").addEventListener("click", applyDatePicker);
    document.querySelector("#previous-card-calendar-month").addEventListener("click", () => {
        calendarViewDate = new Date(
            calendarViewDate.getFullYear(),
            calendarViewDate.getMonth() - 1,
            1
        );
        renderDatePickerCalendar();
    });
    document.querySelector("#next-card-calendar-month").addEventListener("click", () => {
        calendarViewDate = new Date(
            calendarViewDate.getFullYear(),
            calendarViewDate.getMonth() + 1,
            1
        );
        renderDatePickerCalendar();
    });
    datePickerDayInput.addEventListener("change", () => {
        const value = parseDateEntry(datePickerDayInput.value);
        const date = parseInputDate(value);
        if (date) {
            selectedPickerDate = value;
            datePickerDayInput.value = formatDateEntry(value);
            calendarViewDate = new Date(date.getFullYear(), date.getMonth(), 1);
            setDatePickerError();
            renderDatePickerCalendar();
        } else {
            setDatePickerError("Ngày hết hạn phải có định dạng DD/MM/YYYY.");
        }
    });
    datePickerTimeInput.addEventListener("input", () => {
        setDatePickerError();
    });
    [datePickerDayInput, datePickerTimeInput].forEach((input) => {
        input.addEventListener("keydown", (event) => {
            if (event.key === "Enter") {
                event.preventDefault();
                applyDatePicker();
            }
        });
    });
    dialogCompletionButton.addEventListener("click", () => {
        if (activeCard) {
            updateCompletion(activeCard);
        }
    });
    deleteButton.addEventListener("click", () => {
        if (activeCard) {
            deleteCard(activeCard);
        }
    });
    form.addEventListener("submit", saveCard);
    form.addEventListener("click", (event) => {
        const eventPath = event.composedPath();
        if (!datePicker.hidden
                && !eventPath.includes(datePicker)
                && !eventPath.includes(dueDateButton)) {
            closeDatePicker(false);
        }
    });
    sidebarContent.addEventListener("scroll", positionDatePicker);
    dialog.addEventListener("click", (event) => {
        if (event.target === dialog) {
            closeDialog();
        }
    });
    dialog.addEventListener("cancel", (event) => {
        if (!datePicker.hidden) {
            event.preventDefault();
            closeDatePicker();
        }
    });
    dialog.addEventListener("close", () => {
        closeDatePicker(false);
        commentManager.reset();
        activeCard = null;
    });
    window.addEventListener("resize", positionDatePicker);
    window.addEventListener("blur", cancelActiveCardInteraction);
    window.addEventListener("pagehide", cancelActiveCardInteraction);

    return {
        cancelInteraction: cancelActiveCardInteraction,
        initializeList,
        isDraggingOrMoving,
        loadCards,
        removeList,
        renderBoardList
    };
}
