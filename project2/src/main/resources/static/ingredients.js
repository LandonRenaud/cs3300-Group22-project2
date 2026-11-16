(() => {
    const form = document.getElementById("ingredient-form");
    const rows = document.getElementById("ingredient-rows");
    const empty = document.getElementById("ingredient-empty");
    const totals = document.getElementById("nutrition-total");
    const expirySummary = document.getElementById("expiry-summary");
    const errorBox = document.getElementById("ingredient-error");
    const submit = document.getElementById("ingredient-submit");
    const cancel = document.getElementById("ingredient-cancel");
    const preview = document.getElementById("selected-food-facts");
    const extraFields = document.getElementById("extra-nutrients-fields");
    const dialog = document.getElementById("nutrition-dialog");
    let ingredients = [];
    let expiry = new Map();
    let selectedNutrition = null;

    function readNutritionFields() {
        const data = new FormData(form);
        const fields = ["calories", "proteinGrams", "carbsGrams", "fatGrams"];
        const additionalNutrients = {};
        for (const input of extraFields.querySelectorAll("input[data-nutrient-key]")) {
            if (input.value === "") continue;
            const key = input.dataset.nutrientKey;
            const name = input.dataset.nutrientName;
            const unit = input.dataset.nutrientUnit;
            additionalNutrients[key] = { name, amount: Number(input.value), unit };
        }
        const hasMainFacts = fields.some((field) => data.get(field) !== "");
        return hasMainFacts || Object.keys(additionalNutrients).length
            ? { calories: numberOrNull(data.get("calories")) ?? 0,
                proteinGrams: numberOrNull(data.get("proteinGrams")) ?? 0,
                carbsGrams: numberOrNull(data.get("carbsGrams")) ?? 0,
                fatGrams: numberOrNull(data.get("fatGrams")) ?? 0,
                additionalNutrients }
            : null;
    }

    function readForm() {
        const basisAmount = numberOrNull(form.elements.nutritionBasisAmount.value);
        const basisUnit = form.elements.nutritionBasisUnit.value.trim();
        const nutritionBasis = basisAmount && basisUnit
            ? { amount: basisAmount, unit: basisUnit, label: `per ${basisAmount} ${basisUnit}` }
            : selectedNutrition?.nutritionBasis || null;
        return {
            name: form.elements.name.value,
            quantity: numberOrNull(form.elements.quantity.value),
            unit: form.elements.unit.value,
            expirationDate: form.elements.expirationDate.value || null,
            nutritionPerUnit: readNutritionFields(),
            nutritionBasis,
            nutritionSource: selectedNutrition?.nutritionSource || null,
        };
    }

    function resetForm() {
        form.reset();
        document.getElementById("nutrition-customize").open = false;
        form.elements.id.value = "";
        selectedNutrition = null;
        clearExtraFields();
        preview.hidden = true;
        document.getElementById("food-search-input").value = "";
        document.getElementById("food-search-status").textContent = "";
        submit.textContent = "Add ingredient";
        cancel.hidden = true;
        errorBox.textContent = "";
    }

    function clearExtraFields() { extraFields.replaceChildren(); }

    function populateExtraFields(nutrients = {}) {
        clearExtraFields();
        Object.entries(nutrients || {}).forEach(([key, nutrient]) => {
            const label = el("label", {}, `${nutrient.name} (${nutrient.unit})`);
            const input = el("input", {
                type: "number", min: "0", step: "any", value: nutrient.amount,
                "data-nutrient-key": key, "data-nutrient-name": nutrient.name,
                "data-nutrient-unit": nutrient.unit,
            });
            label.append(input);
            extraFields.append(label);
        });
    }

    function renderFacts(target, nutrition, basis) {
        target.replaceChildren();
        const facts = [
            ["Calories", nutrition?.calories, "kcal"],
            ["Protein", nutrition?.proteinGrams, "g"],
            ["Carbohydrates", nutrition?.carbsGrams, "g"],
            ["Fat", nutrition?.fatGrams, "g"],
        ];
        const list = el("dl", { class: "nutrition-facts" });
        for (const [name, value, unit] of facts) {
            if (value == null) continue;
            list.append(el("dt", {}, name), el("dd", {}, `${formatNumber(value)} ${unit}`));
        }
        for (const value of Object.values(nutrition?.additionalNutrients || {})) {
            list.append(el("dt", {}, value.name), el("dd", {}, `${formatNumber(value.amount)} ${value.unit}`));
        }
        target.append(list);
        if (basis) target.append(el("p", { class: "muted" }, `Values ${basis.label || `per ${basis.amount} ${basis.unit}`}.`));
        if (!nutrition || (!facts.some(([, value]) => value != null) && !Object.keys(nutrition.additionalNutrients || {}).length)) {
            target.append(el("p", { class: "muted" }, "No nutrition facts are available for this ingredient."));
        }
    }

    function selectFood(food) {
        selectedNutrition = { nutritionBasis: food.nutritionBasis, nutritionSource: food.nutritionSource };
        form.elements.name.value = food.description;
        const nutrition = food.nutritionPerUnit;
        form.elements.calories.value = nutrition.calories ?? "";
        form.elements.proteinGrams.value = nutrition.proteinGrams ?? "";
        form.elements.carbsGrams.value = nutrition.carbsGrams ?? "";
        form.elements.fatGrams.value = nutrition.fatGrams ?? "";
        form.elements.nutritionBasisAmount.value = food.nutritionBasis.amount;
        form.elements.nutritionBasisUnit.value = food.nutritionBasis.unit;
        populateExtraFields(nutrition.additionalNutrients);
        renderFacts(preview, nutrition, food.nutritionBasis);
        preview.hidden = false;
    }

    function clearSelectedFood() {
        if (!selectedNutrition) return;
        selectedNutrition = null;
        form.elements.calories.value = "";
        form.elements.proteinGrams.value = "";
        form.elements.carbsGrams.value = "";
        form.elements.fatGrams.value = "";
        form.elements.nutritionBasisAmount.value = "";
        form.elements.nutritionBasisUnit.value = "";
        clearExtraFields();
        preview.hidden = true;
    }

    function startEdit(ingredient) {
        form.elements.id.value = ingredient.id;
        form.elements.name.value = ingredient.name;
        form.elements.quantity.value = ingredient.quantity;
        form.elements.unit.value = ingredient.unit;
        form.elements.expirationDate.value = ingredient.expirationDate || "";
        form.elements.nutritionBasisAmount.value = ingredient.nutritionBasis?.amount ?? "";
        form.elements.nutritionBasisUnit.value = ingredient.nutritionBasis?.unit ?? "";
        for (const key of ["calories", "proteinGrams", "carbsGrams", "fatGrams"]) {
            form.elements[key].value = ingredient.nutritionPerUnit?.[key] ?? "";
        }
        populateExtraFields(ingredient.nutritionPerUnit?.additionalNutrients);
        selectedNutrition = {
            nutritionBasis: ingredient.nutritionBasis,
            nutritionSource: ingredient.nutritionSource,
        };
        submit.textContent = "Save changes";
        cancel.hidden = false;
        form.scrollIntoView({ behavior: "smooth" });
    }

    function showIngredientFacts(ingredient) {
        document.getElementById("nutrition-dialog-title").textContent = ingredient.name;
        const content = document.getElementById("nutrition-dialog-content");
        renderFacts(content, ingredient.nutritionPerUnit, ingredient.nutritionBasis);
        if (ingredient.nutritionSource) {
            content.append(el("p", { class: "muted" }, `Source: ${ingredient.nutritionSource.provider === "USDA_FDC" ? "USDA FoodData Central" : ingredient.nutritionSource.provider}`));
        }
        dialog.showModal();
    }

    async function run(action) {
        errorBox.textContent = "";
        try {
            await action();
            document.dispatchEvent(new CustomEvent("ingredients-changed"));
            await refresh();
        }
        catch (err) { errorBox.textContent = err.message; }
    }

    function expiryBadge(item) {
        const info = expiry.get(item.id);
        if (!info || info.status === "NO_DATE") {
            return "—";
        }
        const days = info.daysLeft;
        const label = days < 0 ? `expired ${-days}d ago`
            : days === 0 ? "expires today"
            : `${days}d left`;
        return el("span", { class: `badge ${info.status.toLowerCase().replace("_", "-")}`, title: info.expirationDate },
            label);
    }

    function render() {
        rows.replaceChildren(...ingredients.map((item) => el("tr", {},
            el("td", {}, el("button", { type: "button", class: "ingredient-name", onclick: () => showIngredientFacts(item) }, item.name)),
            el("td", {}, `${formatNumber(item.quantity)} ${item.unit}`),
            el("td", {}, expiryBadge(item)),
            el("td", {}, `${formatNumber(item.nutritionPerUnit.calories)} kcal (${item.nutritionBasis?.label || `per ${item.unit}`})`),
            el("td", { class: "controls" },
                el("button", { class: "small secondary", title: "Used one", onclick: () => run(() => api(`/api/ingredients/${item.id}/quantity`, { method: "PATCH", body: JSON.stringify({ delta: -1 }) })) }, "−1"),
                el("button", { class: "small secondary", title: "Bought one more", onclick: () => run(() => api(`/api/ingredients/${item.id}/quantity`, { method: "PATCH", body: JSON.stringify({ delta: 1 }) })) }, "+1"),
                el("button", { class: "small secondary", onclick: () => startEdit(item) }, "Edit"),
                el("button", { class: "small danger", onclick: () => run(() => api(`/api/ingredients/${item.id}`, { method: "DELETE" })) }, "Remove"),
            ),
        )));
        empty.hidden = ingredients.length > 0;
    }

    async function refresh() {
        ingredients = await api("/api/ingredients");
        const report = await api("/api/ingredients/expiry");
        expiry = new Map(report.map((info) => [info.id, info]));
        render();
        const expired = report.filter((info) => info.status === "EXPIRED").length;
        const soon = report.filter((info) => info.status === "EXPIRING_SOON").length;
        expirySummary.textContent = [
            expired ? `${expired} expired` : "",
            soon ? `${soon} expiring within 3 days` : "",
        ].filter(Boolean).join(" · ");
        const n = await api("/api/ingredients/nutrition");
        totals.textContent = ingredients.length
            ? `On hand in total (compatible units): ${formatNumber(n.calories)} kcal · ${formatNumber(n.proteinGrams)} g protein · `
              + `${formatNumber(n.carbsGrams)} g carbs · ${formatNumber(n.fatGrams)} g fat`
            : "";
    }

    form.addEventListener("submit", (event) => {
        event.preventDefault();
        const id = form.elements.id.value;
        const body = JSON.stringify(readForm());
        run(async () => {
            await (id ? api(`/api/ingredients/${id}`, { method: "PUT", body })
                : api("/api/ingredients", { method: "POST", body }));
            resetForm();
        });
    });

    setupFoodSearch({
        input: document.getElementById("food-search-input"),
        results: document.getElementById("food-search-results"),
        status: document.getElementById("food-search-status"),
        onSelect: selectFood,
        onClear: clearSelectedFood,
    });
    cancel.addEventListener("click", resetForm);
    document.getElementById("nutrition-dialog-close").addEventListener("click", () => dialog.close());
    refresh().catch((err) => { errorBox.textContent = err.message; });
})();
