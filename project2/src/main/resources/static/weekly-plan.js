(() => {
    const form = document.getElementById("weekly-plan-form");
    const submit = document.getElementById("weekly-plan-submit");
    const status = document.getElementById("weekly-plan-status");
    const error = document.getElementById("weekly-plan-error");
    const results = document.getElementById("weekly-plan-results");
    const days = document.getElementById("weekly-plan-days");
    const shopping = document.getElementById("weekly-plan-shopping");
    const save = document.getElementById("weekly-plan-save");
    const snapshotNote = document.getElementById("weekly-plan-snapshot-note");
    const savedList = document.getElementById("saved-weekly-plans-list");
    const savedStatus = document.getElementById("saved-weekly-plans-status");
    const savedError = document.getElementById("saved-weekly-plans-error");
    const refreshSavedButton = document.getElementById("saved-weekly-plans-refresh");
    const today = new Date();
    form.elements.startDate.value = [today.getFullYear(),
        String(today.getMonth() + 1).padStart(2, "0"), String(today.getDate()).padStart(2, "0")].join("-");
    const amount = (value) => value.toLocaleString("en-US", { maximumSignificantDigits: 6 });
    const dateLabel = (value) => new Date(`${value}T12:00:00`).toLocaleDateString("en-US",
        { weekday: "short", month: "short", day: "numeric", year: "numeric" });
    const savedLabel = (value) => new Date(value).toLocaleString("en-US");
    const strategyLabel = (value) => value === "PANTRY_FIRST" ? "Pantry coverage" : "Expiring food first";
    let requestVersion = 0;
    let savedListVersion = 0;
    let attempted = false;
    let displayed = null;
    let openingId = null;
    let saving = false;
    const deleting = new Set();
    const deleted = new Set();

    function updateSaveButton() {
        save.disabled = saving || !displayed || Boolean(displayed.savedId);
        save.textContent = saving ? "Saving..." : displayed?.savedId ? "Saved" : "Save this week";
    }

    function invalidate(message = "") {
        requestVersion++;
        displayed = null;
        openingId = null;
        results.hidden = true;
        days.replaceChildren();
        shopping.replaceChildren();
        snapshotNote.textContent = "";
        error.textContent = "";
        submit.disabled = false;
        status.textContent = message;
        updateSaveButton();
    }

    function render(plan, saved = null) {
        if (!plan.days.length) {
            status.textContent = "Save a meal with at least one positive ingredient quantity, then generate a plan.";
            return;
        }
        displayed = { plan, savedId: saved?.id || null };
        openingId = null;
        days.replaceChildren(...plan.days.map((day) => el("article", { class: "card" },
            el("h3", {}, `${dateLabel(day.date)} - ${day.meal.name}`),
            el("ul", {}, ...day.ingredients.map((line) => el("li", {},
                `${amount(line.needed)} ${line.unit} ${line.name}: `,
                `${amount(line.fromPantry)} from pantry`,
                line.toBuy > 0 ? `; buy ${amount(line.toBuy)}` : ""))),
            day.meal.notes ? el("p", { class: "muted" }, day.meal.notes) : "")));
        shopping.replaceChildren(...(plan.shoppingList.length
            ? plan.shoppingList.map((line) => el("li", {}, `${amount(line.toBuy)} ${line.unit} ${line.name}`))
            : [el("li", {}, saved ? "The pantry covered this plan when it was saved."
                : "Everything needed for this plan is already in your pantry.")]));
        results.hidden = false;
        snapshotNote.textContent = saved
            ? "Saved snapshot: recipes and shopping needs reflect the time of saving. Generate a new plan to use your current pantry."
            : "Saving recalculates this week using your latest recipes and pantry, then keeps a snapshot to open later.";
        status.textContent = saved ? `Saved ${savedLabel(saved.savedAt)}. Inventory is unchanged.`
            : "7 days planned. Pantry quantities are allocated once across the week, using each item by its expiry date.";
        updateSaveButton();
    }

    async function refreshSaved(notice = "") {
        const version = ++savedListVersion;
        savedList.replaceChildren();
        savedList.setAttribute("aria-busy", "true");
        savedError.textContent = "";
        savedStatus.textContent = notice || "Loading saved plans...";
        refreshSavedButton.disabled = true;
        try {
            const snapshots = await api("/api/meal-plans/saved");
            if (version !== savedListVersion) return;
            const visible = snapshots.filter((snapshot) => !deleted.has(snapshot.id));
            savedList.replaceChildren(...visible.map((snapshot) => {
                const open = el("button", { type: "button", class: "small secondary",
                    onclick: () => openSaved(snapshot.id) }, "Open");
                const remove = el("button", { type: "button", class: "small danger",
                    onclick: () => deleteSaved(snapshot.id) }, "Delete");
                const label = `week of ${dateLabel(snapshot.plan.startDate)}, saved ${savedLabel(snapshot.savedAt)}`;
                open.setAttribute("aria-label", `Open ${label}`);
                remove.setAttribute("aria-label", `Delete ${label}`);
                open.disabled = remove.disabled = deleting.has(snapshot.id);
                return el("li", { "data-plan-id": snapshot.id },
                    el("div", {}, el("strong", {}, `Week of ${dateLabel(snapshot.plan.startDate)}`),
                        el("p", { class: "muted" }, `${strategyLabel(snapshot.plan.strategy)} · Saved ${savedLabel(snapshot.savedAt)}`)),
                    el("div", { class: "controls" }, open, remove));
            }));
            savedStatus.textContent = notice || (visible.length
                ? `${visible.length} saved ${visible.length === 1 ? "plan" : "plans"}.`
                : "No saved plans yet. Generate a week, then select Save this week.");
        } catch (err) {
            if (version === savedListVersion) {
                savedStatus.textContent = notice;
                savedError.textContent = `Could not load saved plans: ${err.message} Use Refresh saved plans to try again.`;
            }
        } finally {
            if (version === savedListVersion) {
                savedList.setAttribute("aria-busy", "false");
                refreshSavedButton.disabled = false;
            }
        }
    }

    async function openSaved(id) {
        if (deleting.has(id) || deleted.has(id)) return;
        invalidate("Opening saved plan...");
        attempted = true;
        openingId = id;
        const version = requestVersion;
        try {
            const snapshot = await api(`/api/meal-plans/saved/${encodeURIComponent(id)}`);
            if (version !== requestVersion || deleted.has(id)) return;
            form.elements.startDate.value = snapshot.plan.startDate;
            form.elements.strategy.value = snapshot.plan.strategy;
            render(snapshot.plan, snapshot);
        } catch (err) {
            if (version === requestVersion) {
                status.textContent = "";
                error.textContent = `Could not open saved plan: ${err.message}`;
            }
        } finally {
            if (version === requestVersion) openingId = null;
        }
    }

    function updateDeleteButtons(id) {
        for (const row of savedList.children) {
            if (row.dataset.planId !== id) continue;
            for (const button of row.querySelectorAll("button")) {
                button.disabled = deleting.has(id) || deleted.has(id);
            }
        }
    }

    async function deleteSaved(id) {
        if (deleting.has(id) || deleted.has(id)) return;
        deleting.add(id);
        updateDeleteButtons(id);
        if (openingId === id) invalidate("Deleting saved plan...");
        savedError.textContent = "";
        savedStatus.textContent = "Deleting saved plan...";
        try {
            await api(`/api/meal-plans/saved/${encodeURIComponent(id)}`, { method: "DELETE" });
            deleted.add(id);
            if (displayed?.savedId === id || openingId === id) invalidate("Saved plan deleted.");
            await refreshSaved("Saved plan deleted. Recipes and pantry are unchanged.");
        } catch (err) {
            savedStatus.textContent = "";
            savedError.textContent = `Could not delete saved plan: ${err.message}`;
        } finally {
            deleting.delete(id);
            updateDeleteButtons(id);
        }
    }

    save.addEventListener("click", async () => {
        if (saving || !displayed || displayed.savedId) return;
        const { startDate, strategy } = displayed.plan;
        const version = ++requestVersion;
        saving = true;
        error.textContent = "";
        status.textContent = "Saving this week with your latest recipes and pantry...";
        updateSaveButton();
        try {
            const snapshot = await api("/api/meal-plans/saved", {
                method: "POST", body: JSON.stringify({ startDate, strategy })
            });
            if (version === requestVersion) render(snapshot.plan, snapshot);
            await refreshSaved(`Saved week of ${dateLabel(snapshot.plan.startDate)}.`);
        } catch (err) {
            if (version === requestVersion) {
                status.textContent = "";
                error.textContent = `Could not save this week: ${err.message}`;
            } else {
                savedError.textContent = `Could not save week of ${dateLabel(startDate)}: ${err.message}`;
            }
        } finally {
            saving = false;
            updateSaveButton();
        }
    });

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        invalidate("Generating your weekly plan...");
        attempted = true;
        const version = requestVersion;
        submit.disabled = true;
        const params = new URLSearchParams({ startDate: form.elements.startDate.value,
            strategy: form.elements.strategy.value });
        try {
            const plan = await api(`/api/meal-plans/weekly?${params}`);
            if (version === requestVersion) render(plan);
        } catch (err) {
            if (version === requestVersion) {
                status.textContent = "";
                error.textContent = err.message;
            }
        } finally {
            if (version === requestVersion) submit.disabled = false;
        }
    });
    form.addEventListener("change", () => invalidate(attempted ? "Options changed. Generate a new plan." : ""));
    function inventoryChanged() {
        if (openingId) return;
        if (displayed?.savedId) {
            status.textContent = "Your pantry or recipes changed. This saved snapshot is unchanged; generate a new plan for current shopping needs.";
        } else {
            invalidate(attempted ? "Pantry or recipes changed. Generate a new plan to use the latest data." : "");
        }
    }
    document.addEventListener("ingredients-changed", inventoryChanged);
    document.addEventListener("meals-changed", inventoryChanged);
    refreshSavedButton.addEventListener("click", () => refreshSaved());
    refreshSaved();
})();
