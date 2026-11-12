(() => {
    const form = document.getElementById("meal-form");
    const lines = document.getElementById("meal-lines");
    const list = document.getElementById("meal-list");
    const empty = document.getElementById("meal-empty");
    const search = document.getElementById("meal-search");
    const errorBox = document.getElementById("meal-error");
    const submit = document.getElementById("meal-submit");
    const cancel = document.getElementById("meal-cancel");

    function addLine(item = { name: "", quantity: 1, unit: "" }) {
        const row = el("div", { class: "meal-line" },
            el("input", { "data-field": "name", placeholder: "Ingredient", "aria-label": "Ingredient name" }),
            el("input", { "data-field": "quantity", type: "number", min: "0", step: "any", "aria-label": "Quantity" }),
            el("input", { "data-field": "unit", placeholder: "unit", "aria-label": "Unit" }),
            el("button", { type: "button", class: "small danger", onclick: () => row.remove() }, "✕"),
        );
        row.querySelector('[data-field="name"]').value = item.name;
        row.querySelector('[data-field="quantity"]').value = item.quantity;
        row.querySelector('[data-field="unit"]').value = item.unit;
        lines.append(row);
    }

    function readForm() {
        const ingredients = [...lines.querySelectorAll(".meal-line")]
            .map((row) => ({
                name: row.querySelector('[data-field="name"]').value,
                quantity: numberOrNull(row.querySelector('[data-field="quantity"]').value),
                unit: row.querySelector('[data-field="unit"]').value,
            }))
            .filter((item) => item.name.trim() !== "");
        return { name: form.elements.name.value, notes: form.elements.notes.value, ingredients };
    }

    function resetForm() {
        form.reset();
        form.elements.id.value = "";
        lines.replaceChildren();
        addLine();
        submit.textContent = "Save meal";
        cancel.hidden = true;
        errorBox.textContent = "";
    }

    function startEdit(meal) {
        resetForm();
        form.elements.id.value = meal.id;
        form.elements.name.value = meal.name;
        form.elements.notes.value = meal.notes;
        lines.replaceChildren();
        meal.ingredients.forEach(addLine);
        submit.textContent = "Save changes";
        cancel.hidden = false;
        form.scrollIntoView({ behavior: "smooth" });
    }

    function ingredientItem(line) {
        const text = `${formatNumber(line.needed)} ${line.unit} ${line.name}`;
        if (line.toBuy === 0) {
            return el("li", { class: "have" }, `${text} — have it`);
        }
        const note = line.onHand > 0
            ? `buy ${formatNumber(line.toBuy)} more (have ${formatNumber(line.onHand)})`
            : "need to buy";
        return el("li", { class: "need" }, `${text} — ${note}`);
    }

    async function renderMeal(meal) {
        const shopping = await api(`/api/meals/${meal.id}/shopping-list`);
        const missing = shopping.filter((line) => line.toBuy > 0).length;
        return el("div", { class: "card meal-card" },
            el("div", { class: "header" },
                el("div", {},
                    el("h3", {}, meal.name),
                    el("span", { class: missing ? "need" : "have" },
                        missing ? `${missing} ingredient${missing > 1 ? "s" : ""} to buy` : "Everything on hand"),
                ),
                el("div", { class: "controls" },
                    el("button", { class: "small secondary", onclick: () => startEdit(meal) }, "Edit"),
                    el("button", {
                        class: "small danger",
                        onclick: () => run(() => api(`/api/meals/${meal.id}`, { method: "DELETE" })),
                    }, "Delete"),
                ),
            ),
            el("ul", {}, ...shopping.map(ingredientItem)),
            meal.notes ? el("p", { class: "muted" }, meal.notes) : "",
        );
    }

    async function refresh() {
        const query = search.value.trim();
        const meals = await api(`/api/meals${query ? `?q=${encodeURIComponent(query)}` : ""}`);
        list.replaceChildren(...await Promise.all(meals.map(renderMeal)));
        empty.hidden = meals.length > 0;
        empty.textContent = query ? "No meals match your search." : "No saved meals yet.";
    }

    async function run(action) {
        errorBox.textContent = "";
        try {
            await action();
            await refresh();
        } catch (err) {
            errorBox.textContent = err.message;
        }
    }

    form.addEventListener("submit", (event) => {
        event.preventDefault();
        const id = form.elements.id.value;
        const body = JSON.stringify(readForm());
        run(async () => {
            await (id
                ? api(`/api/meals/${id}`, { method: "PUT", body })
                : api("/api/meals", { method: "POST", body }));
            resetForm();
        });
    });

    document.getElementById("meal-add-line").addEventListener("click", () => addLine());
    cancel.addEventListener("click", resetForm);

    let searchTimer;
    search.addEventListener("input", () => {
        clearTimeout(searchTimer);
        searchTimer = setTimeout(() => run(() => Promise.resolve()), 200);
    });

    // Pantry changes affect what each meal still needs, so re-render when ingredients change.
    document.addEventListener("ingredients-changed", () => run(() => Promise.resolve()));

    resetForm();
    refresh().catch((err) => { errorBox.textContent = err.message; });
})();
