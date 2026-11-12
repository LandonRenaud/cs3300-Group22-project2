// Small fetch wrapper shared by the feature scripts. Throws an Error carrying the server's problem "detail".
async function api(path, options = {}) {
    const response = await fetch(path, {
        headers: { "Content-Type": "application/json" },
        ...options,
    });
    if (response.status === 204) {
        return null;
    }
    const body = await response.json().catch(() => null);
    if (!response.ok) {
        throw new Error((body && body.detail) || `Request failed (${response.status})`);
    }
    return body;
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
