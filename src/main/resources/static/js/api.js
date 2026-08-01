export class ApiError extends Error {
    constructor(message, status, payload) {
        super(message);
        this.name = "ApiError";
        this.status = status;
        this.payload = payload;
    }
}

function csrfHeaders(method) {
    if (["GET", "HEAD", "OPTIONS", "TRACE"].includes(method.toUpperCase())) {
        return {};
    }
    const token = document.querySelector('meta[name="_csrf"]')?.content;
    const header = document.querySelector('meta[name="_csrf_header"]')?.content;
    return token && header ? { [header]: token } : {};
}

async function parseResponse(response) {
    if (response.status === 204) {
        return null;
    }
    const text = await response.text();
    if (!text) {
        return null;
    }
    const contentType = response.headers.get("content-type") || "";
    if (contentType.includes("application/json")) {
        try {
            return JSON.parse(text);
        } catch (error) {
            throw new ApiError("Server returned invalid JSON", response.status, { cause: error });
        }
    }
    return text;
}

export async function request(url, { method = "GET", body, headers = {} } = {}) {
    const normalizedMethod = method.toUpperCase();
    const requestHeaders = new Headers({ ...headers, ...csrfHeaders(normalizedMethod) });
    const options = { method: normalizedMethod, headers: requestHeaders, credentials: "same-origin" };

    if (body !== undefined && body !== null) {
        if (body instanceof FormData || body instanceof URLSearchParams || typeof body === "string") {
            options.body = body;
        } else {
            requestHeaders.set("Content-Type", "application/json");
            options.body = JSON.stringify(body);
        }
    }

    const response = await fetch(url, options);
    const payload = await parseResponse(response);
    if (!response.ok) {
        const message = payload && typeof payload === "object" && payload.message
            ? payload.message
            : `Request failed with status ${response.status}`;
        throw new ApiError(message, response.status, payload);
    }
    return payload;
}

export const api = {
    get: (url, options = {}) => request(url, { ...options, method: "GET" }),
    post: (url, body, options = {}) => request(url, { ...options, method: "POST", body }),
    put: (url, body, options = {}) => request(url, { ...options, method: "PUT", body }),
    patch: (url, body, options = {}) => request(url, { ...options, method: "PATCH", body }),
    delete: (url, options = {}) => request(url, { ...options, method: "DELETE" })
};
