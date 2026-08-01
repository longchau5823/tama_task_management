import { api, ApiError } from "./api.js";
import {
    showErrorToast,
    showSuccessToast
} from "./toast.js";

export function createCommentManager({ errorMessage }) {
    const section = document.querySelector("#card-comments-section");
    const loadingElement = document.querySelector("#card-comments-loading");
    const errorElement = document.querySelector("#card-comments-error");
    const listElement = document.querySelector("#card-comments-list");
    const composer = document.querySelector("#card-comment-composer");
    const contentInput = document.querySelector("#card-comment-content");
    const formError = document.querySelector("#card-comment-form-error");
    const addButton = document.querySelector("#add-card-comment");
    const retryButton = document.querySelector("#retry-card-comments");
    const scrollArea = section.closest(".card-dialog-main-scroll");

    const busyCommentIds = new Set();
    let comments = [];
    let currentCardId = null;
    let currentUser = null;
    let currentUserPromise = null;
    let loadError = null;
    let loading = false;
    let createBusy = false;
    let editingCommentId = null;
    let requestVersion = 0;

    function getCurrentUser() {
        if (currentUser) {
            return Promise.resolve(currentUser);
        }
        if (!currentUserPromise) {
            currentUserPromise = api.get("/api/auth/me")
                .then((user) => {
                    currentUser = user;
                    return user;
                })
                .finally(() => {
                    currentUserPromise = null;
                });
        }
        return currentUserPromise;
    }

    function formatCommentTime(value) {
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return value || "";
        }
        return date.toLocaleString("vi-VN", {
            day: "2-digit",
            month: "2-digit",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit"
        });
    }

    function sortCommentsOldestFirst(items) {
        return [...items].sort((left, right) => {
            const leftTime = new Date(left.createdAt).getTime();
            const rightTime = new Date(right.createdAt).getTime();
            if (!Number.isNaN(leftTime)
                    && !Number.isNaN(rightTime)
                    && leftTime !== rightTime) {
                return leftTime - rightTime;
            }
            return left.id - right.id;
        });
    }

    function scrollToLatestComment() {
        window.requestAnimationFrame(() => {
            if (scrollArea) {
                scrollArea.scrollTo({
                    top: scrollArea.scrollHeight,
                    behavior: "smooth"
                });
            }
        });
    }

    function resizeContentInput() {
        contentInput.style.height = "auto";
        const maxHeight = Number.parseFloat(
            window.getComputedStyle(contentInput).maxHeight
        );
        const naturalHeight = contentInput.scrollHeight;
        const nextHeight = Number.isFinite(maxHeight)
            ? Math.min(naturalHeight, maxHeight)
            : naturalHeight;
        contentInput.style.height = `${nextHeight}px`;
        contentInput.style.overflowY =
            Number.isFinite(maxHeight) && naturalHeight > maxHeight
                ? "auto"
                : "hidden";
    }

    function showFormError(message) {
        formError.textContent = message;
        formError.hidden = false;
    }

    function hideFormError() {
        formError.textContent = "";
        formError.hidden = true;
    }

    function createActionButton(text, className, action, disabled = false) {
        const button = document.createElement("button");
        button.type = "button";
        button.className = className;
        button.textContent = text;
        button.disabled = disabled;
        button.addEventListener("click", action);
        return button;
    }

    function isOwnComment(comment) {
        return currentUser && comment.authorId === currentUser.id;
    }

    function createCommentEditor(comment) {
        const editor = document.createElement("div");
        editor.className = "card-comment-editor";

        const textarea = document.createElement("textarea");
        textarea.rows = 4;
        textarea.value = comment.content;
        textarea.setAttribute("aria-label", `Sửa bình luận của ${comment.authorUsername}`);

        const error = document.createElement("p");
        error.className = "card-comment-form-error";
        error.setAttribute("role", "alert");
        error.hidden = true;

        const actions = document.createElement("div");
        actions.className = "card-comment-editor-actions";
        const cancelButton = createActionButton(
            "Hủy",
            "button button-secondary",
            () => {
                editingCommentId = null;
                render();
            }
        );
        const saveButton = createActionButton(
            "Lưu",
            "button button-primary",
            () => updateComment(comment, textarea, error, saveButton, cancelButton)
        );
        actions.append(cancelButton, saveButton);
        editor.append(textarea, error, actions);

        window.requestAnimationFrame(() => {
            textarea.focus();
            textarea.setSelectionRange(textarea.value.length, textarea.value.length);
        });
        return editor;
    }

    function createCommentElement(comment) {
        const article = document.createElement("article");
        article.className = "card-comment";
        article.dataset.commentId = String(comment.id);

        const heading = document.createElement("header");
        heading.className = "card-comment-heading";

        const author = document.createElement("strong");
        author.textContent = comment.authorUsername;

        const metadata = document.createElement("span");
        metadata.className = "card-comment-metadata";
        metadata.textContent = formatCommentTime(comment.createdAt);
        if (comment.edited) {
            const edited = document.createElement("span");
            edited.className = "card-comment-edited";
            edited.textContent = `Đã chỉnh sửa ${formatCommentTime(comment.updatedAt)}`;
            metadata.append(" · ", edited);
        }
        heading.append(author, metadata);
        article.append(heading);

        if (editingCommentId === comment.id) {
            article.append(createCommentEditor(comment));
            return article;
        }

        const content = document.createElement("p");
        content.className = "card-comment-content";
        content.textContent = comment.content;
        article.append(content);

        if (isOwnComment(comment)) {
            const actions = document.createElement("div");
            actions.className = "card-comment-actions";
            const busy = busyCommentIds.has(comment.id);
            actions.append(
                createActionButton("Sửa", "card-comment-action", () => {
                    editingCommentId = comment.id;
                    render();
                }, busy),
                createActionButton("Xóa", "card-comment-action is-delete", () => {
                    void deleteComment(comment);
                }, busy)
            );
            article.append(actions);
        }
        return article;
    }

    function render() {
        section.hidden = currentCardId === null;
        loadingElement.hidden = !loading;
        errorElement.hidden = loadError === null;
        composer.hidden = currentCardId === null || loading || loadError !== null;
        listElement.replaceChildren();

        if (loadError) {
            errorElement.querySelector("p").textContent = errorMessage(
                loadError,
                "Không thể tải bình luận."
            );
        }

        if (!loading && !loadError) {
            listElement.replaceChildren(...comments.map(createCommentElement));
        }
        addButton.disabled = createBusy;
        contentInput.disabled = createBusy;
        if (!composer.hidden) {
            window.requestAnimationFrame(resizeContentInput);
        }
    }

    async function openCard(cardId) {
        const version = ++requestVersion;
        currentCardId = cardId;
        comments = [];
        loadError = null;
        loading = true;
        createBusy = false;
        editingCommentId = null;
        busyCommentIds.clear();
        contentInput.value = "";
        hideFormError();
        render();

        try {
            const [loadedComments] = await Promise.all([
                api.get(`/api/cards/${cardId}/comments`),
                getCurrentUser()
            ]);
            if (version !== requestVersion || currentCardId !== cardId) {
                return;
            }
            comments = sortCommentsOldestFirst(loadedComments);
        } catch (error) {
            if (version !== requestVersion || currentCardId !== cardId) {
                return;
            }
            loadError = error;
            if (!(error instanceof ApiError)) {
                console.error(error);
            }
        } finally {
            if (version === requestVersion && currentCardId === cardId) {
                loading = false;
                render();
            }
        }
    }

    async function createComment() {
        const content = contentInput.value.trim();
        if (!currentCardId || createBusy) {
            return;
        }
        if (!content) {
            showFormError("Nội dung bình luận không được để trống.");
            contentInput.focus();
            return;
        }

        const cardId = currentCardId;
        const version = requestVersion;
        createBusy = true;
        hideFormError();
        render();
        try {
            const created = await api.post(`/api/cards/${cardId}/comments`, { content });
            if (version !== requestVersion || currentCardId !== cardId) {
                return;
            }
            comments = sortCommentsOldestFirst([...comments, created]);
            contentInput.value = "";
            render();
            scrollToLatestComment();
            showSuccessToast("Đã thêm bình luận.");
        } catch (error) {
            if (version === requestVersion && currentCardId === cardId) {
                showErrorToast(errorMessage(error, "Không thể thêm bình luận."));
            }
            if (!(error instanceof ApiError)) {
                console.error(error);
            }
        } finally {
            if (version === requestVersion && currentCardId === cardId) {
                createBusy = false;
                render();
            }
        }
    }

    async function updateComment(comment, textarea, error, saveButton, cancelButton) {
        const content = textarea.value.trim();
        if (!content) {
            error.textContent = "Nội dung bình luận không được để trống.";
            error.hidden = false;
            textarea.focus();
            return;
        }

        const cardId = currentCardId;
        const version = requestVersion;
        busyCommentIds.add(comment.id);
        textarea.disabled = true;
        saveButton.disabled = true;
        cancelButton.disabled = true;
        try {
            const updated = await api.put(`/api/comments/${comment.id}`, { content });
            if (version !== requestVersion || currentCardId !== cardId) {
                return;
            }
            comments = sortCommentsOldestFirst(
                comments.map((item) => item.id === updated.id ? updated : item)
            );
            editingCommentId = null;
            render();
            showSuccessToast("Đã cập nhật bình luận.");
        } catch (requestError) {
            if (version === requestVersion && currentCardId === cardId) {
                error.textContent = errorMessage(
                    requestError,
                    "Không thể cập nhật bình luận."
                );
                error.hidden = false;
                textarea.disabled = false;
                saveButton.disabled = false;
                cancelButton.disabled = false;
                showErrorToast(error.textContent);
            }
            if (!(requestError instanceof ApiError)) {
                console.error(requestError);
            }
        } finally {
            busyCommentIds.delete(comment.id);
            if (version === requestVersion
                    && currentCardId === cardId
                    && editingCommentId === null) {
                render();
            }
        }
    }

    async function deleteComment(comment) {
        if (busyCommentIds.has(comment.id)
                || !window.confirm("Xóa bình luận này? Thao tác này không thể khôi phục.")) {
            return;
        }

        const cardId = currentCardId;
        const version = requestVersion;
        busyCommentIds.add(comment.id);
        render();
        try {
            await api.delete(`/api/comments/${comment.id}`);
            if (version !== requestVersion || currentCardId !== cardId) {
                return;
            }
            comments = comments.filter((item) => item.id !== comment.id);
            if (editingCommentId === comment.id) {
                editingCommentId = null;
            }
            render();
            showSuccessToast("Đã xóa bình luận.");
        } catch (error) {
            if (version === requestVersion && currentCardId === cardId) {
                showErrorToast(errorMessage(error, "Không thể xóa bình luận."));
            }
            if (!(error instanceof ApiError)) {
                console.error(error);
            }
        } finally {
            busyCommentIds.delete(comment.id);
            if (version === requestVersion && currentCardId === cardId) {
                render();
            }
        }
    }

    function reset() {
        requestVersion++;
        currentCardId = null;
        comments = [];
        loadError = null;
        loading = false;
        createBusy = false;
        editingCommentId = null;
        busyCommentIds.clear();
        contentInput.value = "";
        hideFormError();
        render();
    }

    addButton.addEventListener("click", () => {
        void createComment();
    });
    contentInput.addEventListener("input", () => {
        hideFormError();
        resizeContentInput();
    });
    contentInput.addEventListener("keydown", (event) => {
        if ((event.ctrlKey || event.metaKey) && event.key === "Enter") {
            event.preventDefault();
            void createComment();
        }
    });
    retryButton.addEventListener("click", () => {
        if (currentCardId) {
            void openCard(currentCardId);
        }
    });

    reset();
    return { openCard, reset };
}
