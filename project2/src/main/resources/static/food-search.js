/* USDA autocomplete stays separate from ingredient CRUD; selected data is returned for the ingredient form. */
function setupFoodSearch({ input, results, status, onSelect, onClear }) {
    let timer;
    let sequence = 0;

    function clearResults() { results.replaceChildren(); results.hidden = true; }

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
                if (!matches.length) {
                    status.textContent = "No matching foods found. You can enter the food manually.";
                    return;
                }
                status.textContent = "";
                results.hidden = false;
                for (const item of matches) {
                    const label = item.brandOwner
                        ? `${item.description} — ${item.brandOwner}` : item.description;
                    const preview = item.caloriesPer100g == null ? "" : ` · ${formatNumber(item.caloriesPer100g)} kcal/100 g`;
                    results.append(el("button", {
                        type: "button", role: "option", class: "autocomplete-option",
                        onclick: async () => {
                            clearResults();
                            status.textContent = "Loading nutrition facts…";
                            try {
                                const details = await api(`/api/foods/${encodeURIComponent(item.foodId)}`);
                                status.textContent = "USDA FoodData Central · select Customize nutrition facts to edit imported values.";
                                onSelect(details);
                            } catch (error) {
                                status.textContent = error.message;
                            }
                        },
                    }, `${label}${preview}`));
                }
            } catch (error) {
                if (requestId === sequence) status.textContent = error.message;
            }
        }, 300);
    });

    document.addEventListener("click", (event) => {
        if (!event.target.closest(".food-search")) clearResults();
    });
}
