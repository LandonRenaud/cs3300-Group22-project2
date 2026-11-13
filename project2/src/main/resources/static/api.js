// Small fetch wrapper shared by the feature scripts. Throws an Error carrying the server's problem "detail".
async function api(path, options = {}) {
    const response = await authorizedFetch(path, options, false);
    if (response.status === 204) {
        return null;
    }
    const body = await response.json().catch(() => null);
    if (!response.ok) {
        throw new Error((body && body.detail) || `Request failed (${response.status})`);
    }
    return body;
}

async function authorizedFetch(path, options, retried) {
    const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
    const token = localStorage.getItem("mealprep.idToken");
    if (token) headers.Authorization = `Bearer ${token}`;
    const response = await fetch(path, { ...options, headers });
    if (response.status !== 401 || retried || path.startsWith("/api/auth/")) return response;

    const refreshToken = localStorage.getItem("mealprep.refreshToken");
    if (!refreshToken) return response;
    try {
        const refreshed = await fetch("/api/auth/refresh", {
            method: "POST", headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ refreshToken }),
        });
        if (!refreshed.ok) throw new Error("refresh failed");
        const session = await refreshed.json();
        localStorage.setItem("mealprep.idToken", session.idToken);
        localStorage.setItem("mealprep.refreshToken", session.refreshToken);
        return authorizedFetch(path, options, true);
    } catch (_) {
        localStorage.removeItem("mealprep.idToken");
        localStorage.removeItem("mealprep.refreshToken");
        location.replace("/Welcome.html");
        return response;
    }
}

function numberOrNull(value) {
    return value === "" || value == null ? null : Number(value);
}

function formatNumber(value) {
    return Number.isInteger(value) ? String(value) : value.toFixed(1);
}

function el(tag, attrs = {}, ...children) {
    const node = document.createElement(tag);
    for (const [key, value] of Object.entries(attrs)) {
        if (key.startsWith("on")) {
            node.addEventListener(key.slice(2), value);
        } else {
            node.setAttribute(key, value);
        }
    }
    node.append(...children);
    return node;
}
