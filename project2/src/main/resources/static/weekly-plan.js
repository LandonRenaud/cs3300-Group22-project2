(() => {
    const form = document.getElementById("weekly-plan-form");
    const submit = document.getElementById("weekly-plan-submit");
    const status = document.getElementById("weekly-plan-status");
    const error = document.getElementById("weekly-plan-error");
    const results = document.getElementById("weekly-plan-results");
    const days = document.getElementById("weekly-plan-days");
    const shopping = document.getElementById("weekly-plan-shopping");
    const today = new Date();
    form.elements.startDate.value = [today.getFullYear(),
        String(today.getMonth() + 1).padStart(2, "0"), String(today.getDate()).padStart(2, "0")].join("-");
    const amount = (value) => value.toLocaleString("en-US", { maximumSignificantDigits: 6 });
    const dateLabel = (value) => new Date(`${value}T12:00:00`).toLocaleDateString("en-US",
        { weekday: "short", month: "short", day: "numeric" });
    let requestVersion = 0;
    let attempted = false;

    function invalidate() {
        requestVersion++;
        results.hidden = true;
        days.replaceChildren();
        shopping.replaceChildren();
        error.textContent = "";
        submit.disabled = false;
        if (attempted) status.textContent = "Inputs changed. Generate a new plan to use the latest meals and inventory.";
    }

    function render(plan) {
        if (!plan.days.length) {
            status.textContent = "Save a meal with at least one positive ingredient quantity, then generate a plan.";
            return;
        }
        days.replaceChildren(...plan.days.map((day) => el("article", { class: "card" },
            el("h3", {}, `${dateLabel(day.date)} - ${day.meal.name}`),
            el("ul", {}, ...day.ingredients.map((line) => el("li", {},
                `${amount(line.needed)} ${line.unit} ${line.name}: `,
                `${amount(line.fromPantry)} from pantry`,
                line.toBuy > 0 ? `; buy ${amount(line.toBuy)}` : ""))),
            day.meal.notes ? el("p", { class: "muted" }, day.meal.notes) : "")));
        shopping.replaceChildren(...(plan.shoppingList.length
            ? plan.shoppingList.map((line) => el("li", {}, `${amount(line.toBuy)} ${line.unit} ${line.name}`))
            : [el("li", {}, "Everything needed for this plan is already in your pantry.")]));
        results.hidden = false;
        status.textContent = "7 days planned. Pantry quantities are allocated once across the week, using each item by its expiry date.";
    }

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        invalidate();
        attempted = true;
        const version = requestVersion;
        submit.disabled = true;
        status.textContent = "Generating your weekly plan...";
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
    form.addEventListener("change", invalidate);
    document.addEventListener("ingredients-changed", invalidate);
    document.addEventListener("meals-changed", invalidate);
})();
