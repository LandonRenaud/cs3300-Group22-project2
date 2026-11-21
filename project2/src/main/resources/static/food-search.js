/* Shared USDA food autocomplete for pantry and meal ingredient forms. */
function setupFoodSearch({ input, results, status, onSelect, onClear = () => {}, loadDetails = true }) {
    let timer;
    let sequence = 0;
    const searchRoot = input.closest(".food-search, .meal-food-search") || input.parentElement;

    function clearResults() {
        results.replaceChildren();
        results.hidden = true;
    }

    function addResults(title, matches, query) {
        if (!matches.length) return;
        results.append(el("div", { class: "autocomplete-group-label" }, title));
        for (const item of matches) {
            const label = item.brandOwner ? `${item.description} — ${item.brandOwner}` : item.description;
            const preview = item.caloriesPer100g == null
                ? ""
                : ` · ${formatNumber(item.caloriesPer100g)} kcal/100 g`;
            results.append(el("button", {
                type: "button", role: "option", class: "autocomplete-option",
                onclick: async () => {
                    clearResults();
                    if (!loadDetails) {
                        onSelect(item, query);
                        return;
                    }
                    status.textContent = "Loading nutrition facts…";
                    try {
                        const details = await api(`/api/foods/${encodeURIComponent(item.foodId)}`);
                        status.textContent = "USDA FoodData Central · select Customize nutrition facts to edit imported values.";
                        onSelect(details, query, item);
                    } catch (error) {
                        status.textContent = error.message;
                    }
                },
            }, `${label}${preview}`));
        }
    }

    input.addEventListener("input", () => {
        clearTimeout(timer);
        const query = input.value.trim();
        onClear();
        clearResults();
        status.textContent = query.length < 2 ? "" : "Searching…";
        if (query.length < 2) return;
        const requestId = ++sequence;
        timer = setTimeout(async () => {
            try {
                const matches = await api(`/api/foods/search?q=${encodeURIComponent(query)}`);
                if (requestId !== sequence || input.value.trim() !== query) return;
                clearResults();
                const generic = matches.filter((item) => item.dataType !== "Branded");
                const branded = matches.filter((item) => item.dataType === "Branded");
                if (!generic.length && !branded.length) {
                    status.textContent = "No matching foods found. You can enter the food manually.";
                    return;
                }
                status.textContent = "";
                results.hidden = false;
                addResults("Generic foods", generic, query);
                addResults("Branded products", branded, query);
            } catch (error) {
                if (requestId === sequence) status.textContent = error.message;
            }
        }, 700);
    });

    results.addEventListener("mousedown", (event) => event.preventDefault());
    input.addEventListener("blur", () => setTimeout(() => {
        if (!searchRoot.contains(document.activeElement)) clearResults();
    }, 120));

    return () => {
        clearTimeout(timer);
        sequence++;
        clearResults();
    };
}
