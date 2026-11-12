(() => {
    const form = document.getElementById("ingredient-form");
    const rows = document.getElementById("ingredient-rows");
    const empty = document.getElementById("ingredient-empty");
    const totals = document.getElementById("nutrition-total");
    const errorBox = document.getElementById("ingredient-error");
    const submit = document.getElementById("ingredient-submit");
    const cancel = document.getElementById("ingredient-cancel");

    let ingredients = [];

    function readForm() {
        const data = new FormData(form);
        const nutritionFields = ["calories", "proteinGrams", "carbsGrams", "fatGrams"];
        const hasNutrition = nutritionFields.some((f) => data.get(f) !== "");
        return {
            name: data.get("name"),
            quantity: numberOrNull(data.get("quantity")),
            unit: data.get("unit"),
            expirationDate: data.get("expirationDate") || null,
            nutritionPerUnit: hasNutrition
                ? Object.fromEntries(nutritionFields.map((f) => [f, numberOrNull(data.get(f)) ?? 0]))
                : null,
        };
    }

    function resetForm() {
        form.reset();
        form.elements.id.value = "";
        submit.textContent = "Add ingredient";
        cancel.hidden = true;
        errorBox.textContent = "";
    }

    function startEdit(ingredient) {
        form.elements.id.value = ingredient.id;
        form.elements.name.value = ingredient.name;
        form.elements.quantity.value = ingredient.quantity;
        form.elements.unit.value = ingredient.unit;
        form.elements.expirationDate.value = ingredient.expirationDate || "";
        for (const [key, value] of Object.entries(ingredient.nutritionPerUnit)) {
            form.elements[key].value = value || "";
        }
        submit.textContent = "Save changes";
        cancel.hidden = false;
        form.scrollIntoView({ behavior: "smooth" });
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

    function render() {
        rows.replaceChildren(...ingredients.map((item) => el("tr", {},
            el("td", {}, item.name),
            el("td", {}, `${formatNumber(item.quantity)} ${item.unit}`),
            el("td", {}, item.expirationDate || "—"),
            el("td", {}, formatNumber(item.nutritionPerUnit.calories * item.quantity)),
            el("td", { class: "controls" },
                el("button", {
                    class: "small secondary", title: "Used one",
                    onclick: () => run(() => api(`/api/ingredients/${item.id}/quantity`, {
                        method: "PATCH", body: JSON.stringify({ delta: -1 }),
                    })),
                }, "−1"),
                el("button", {
                    class: "small secondary", title: "Bought one more",
                    onclick: () => run(() => api(`/api/ingredients/${item.id}/quantity`, {
                        method: "PATCH", body: JSON.stringify({ delta: 1 }),
                    })),
                }, "+1"),
                el("button", { class: "small secondary", onclick: () => startEdit(item) }, "Edit"),
                el("button", {
                    class: "small danger",
                    onclick: () => run(() => api(`/api/ingredients/${item.id}`, { method: "DELETE" })),
                }, "Remove"),
            ),
        )));
        empty.hidden = ingredients.length > 0;
    }

    async function refresh() {
        ingredients = await api("/api/ingredients");
        render();
        const n = await api("/api/ingredients/nutrition");
        totals.textContent = ingredients.length
            ? `On hand in total: ${formatNumber(n.calories)} kcal · ${formatNumber(n.proteinGrams)} g protein · `
              + `${formatNumber(n.carbsGrams)} g carbs · ${formatNumber(n.fatGrams)} g fat`
            : "";
        document.dispatchEvent(new CustomEvent("ingredients-changed", { detail: ingredients }));
    }

    form.addEventListener("submit", (event) => {
        event.preventDefault();
        const id = form.elements.id.value;
        const body = JSON.stringify(readForm());
        run(async () => {
            await (id
                ? api(`/api/ingredients/${id}`, { method: "PUT", body })
                : api("/api/ingredients", { method: "POST", body }));
            resetForm();
        });
    });

    cancel.addEventListener("click", resetForm);

    refresh().catch((err) => { errorBox.textContent = err.message; });
})();
