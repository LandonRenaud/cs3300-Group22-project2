(() => {
    const TOKEN = "mealprep.idToken";
    const REFRESH = "mealprep.refreshToken";
    const page = document.body.dataset.page;
    let sessionCheckMessage = "";

    function clearSession() {
        localStorage.removeItem(TOKEN);
        localStorage.removeItem(REFRESH);
    }

    function saveSession(session) {
        localStorage.setItem(TOKEN, session.idToken);
        localStorage.setItem(REFRESH, session.refreshToken);
    }

    async function postAuth(path, body) {
        const response = await fetch(path, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(body),
        });
        const result = await response.json().catch(() => ({}));
        if (!response.ok) {
            const message = result.detail || result.message || result.error
                || `Request failed (${response.status})`;
            console.error(`[auth] ${path} failed`, {
                status: response.status,
                message,
                response: result,
            });
            throw new Error(message);
        }
        return result;
    }

    async function refreshSession() {
        const refreshToken = localStorage.getItem(REFRESH);
        if (!refreshToken) return false;
        try {
            saveSession(await postAuth("/api/auth/refresh", { refreshToken }));
            return true;
        } catch (err) {
            sessionCheckMessage = err.message;
            console.error("[auth] Could not refresh Firebase session", err);
            clearSession();
            return false;
        }
    }

    async function hasValidSession() {
        sessionCheckMessage = "";
        if (!localStorage.getItem(TOKEN)) {
            const refreshed = await refreshSession();
            if (!refreshed) return false;
        }

        try {
            let response = await fetch("/api/auth/me", {
                headers: { Authorization: `Bearer ${localStorage.getItem(TOKEN)}` },
            });
            if (response.status === 401 && await refreshSession()) {
                response = await fetch("/api/auth/me", {
                    headers: { Authorization: `Bearer ${localStorage.getItem(TOKEN)}` },
                });
            }
            if (response.ok) return true;
            if (response.status === 401) {
                clearSession();
                sessionCheckMessage = "Your sign-in has expired. Please log in again.";
            } else {
                sessionCheckMessage = `The app could not validate your session (HTTP ${response.status}). Check the Firebase configuration and server logs.`;
            }
            console.error("[auth] Session validation failed", {
                status: response.status,
                message: sessionCheckMessage,
            });
            return false;
        } catch (err) {
            sessionCheckMessage = `The app could not validate your session: ${err.message}`;
            console.error("[auth] Session validation request failed", err);
            return false;
        }
    }

    async function loadAppScripts() {
        for (const src of ["api.js", "ingredients.js", "meals.js"]) {
            await new Promise((resolve, reject) => {
                const script = document.createElement("script");
                script.src = src;
                script.onload = resolve;
                script.onerror = () => reject(new Error(`Could not load ${src}`));
                document.body.append(script);
            });
        }
    }

    async function handlePublicPage() {
        if (await hasValidSession()) {
            location.replace("/Home.html");
            return;
        }
        clearSession();

        const main = document.querySelector("main");
        main.hidden = false;
        if (page === "welcome") return;

        const form = document.getElementById(page === "login" ? "login-form" : "signup-form");
        const error = document.getElementById("auth-error");
        form.addEventListener("submit", async (event) => {
            event.preventDefault();
            error.textContent = "";
            const data = new FormData(form);
            const endpoint = page === "signup" ? "/api/auth/register" : "/api/auth/login";
            try {
                // Firebase signup returns an ID token, so the new account is signed in immediately.
                const session = await postAuth(endpoint, {
                    email: data.get("email"),
                    password: data.get("password"),
                });
                if (!session.idToken || !session.refreshToken) {
                    throw new Error("Firebase did not return a complete sign-in session.");
                }
                saveSession(session);
                if (!await hasValidSession()) {
                    clearSession();
                    error.textContent = sessionCheckMessage || "The app could not validate the new session.";
                    return;
                }
                location.replace("/Home.html");
            } catch (err) {
                clearSession();
                console.error(`[auth] ${page === "signup" ? "Sign-up" : "Login"} failed`, err);
                error.textContent = err.message;
            }
        });
    }

    async function handleHomePage() {
        if (!await hasValidSession()) {
            clearSession();
            location.replace("/login.html");
            return;
        }

        document.querySelector("header").hidden = false;
        document.getElementById("home-content").hidden = false;
        document.getElementById("logout").addEventListener("click", () => {
            clearSession();
            location.replace("/Welcome.html");
        });
        try {
            await loadAppScripts();
        } catch (err) {
            const error = document.getElementById("ingredient-error");
            error.textContent = err.message;
        }
    }

    if (page === "home") handleHomePage();
    else handlePublicPage();
})();
