import { api, ApiError } from "./api.js";

const form = document.querySelector("#registration-form");
const message = document.querySelector("#registration-message");

if (form && message) {
    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const submitButton = form.querySelector('button[type="submit"]');
        submitButton.disabled = true;
        message.hidden = true;

        const fields = new FormData(form);
        try {
            await api.post("/api/auth/register", {
                username: fields.get("username"),
                email: fields.get("email"),
                password: fields.get("password")
            });
            window.location.assign("/login?registered");
        } catch (error) {
            message.textContent = error instanceof ApiError ? error.message : "Không thể hoàn tất đăng ký.";
            message.className = "notice notice-error";
            message.hidden = false;
            if (!(error instanceof ApiError)) {
                console.error(error);
            }
        } finally {
            submitButton.disabled = false;
        }
    });
}
