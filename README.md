# cs3300-Group22-project2

## Build and run locally

The app uses Java 21 and Maven. The Maven wrapper is included, so a separate Maven installation is not required.

### 1. Configure Firebase once

Use the Firebase project attached to the same Google Cloud project as the website:

1. In Firebase Console, enable **Authentication > Sign-in method > Email/Password**.
2. Create a **Realtime Database** instance. Copy its database URL from the Realtime Database page; it looks like `https://<database-name>.<region>.firebasedatabase.app` or `https://<database-name>-default-rtdb.firebaseio.com`.
3. In **Realtime Database > Rules**, publish rules that restrict each user's data to their authenticated Firebase UID:

   ```json
   {
     "rules": {
       ".read": false,
       ".write": false,
       "users": {
         "$uid": {
           ".read": "auth != null && auth.uid === $uid",
           ".write": "auth != null && auth.uid === $uid"
         }
       }
     }
   }
   ```

   The app stores data at `/users/{uid}/ingredients/{id}` and `/users/{uid}/meals/{id}`. The backend sends the signed-in user's Firebase ID token with each Realtime Database REST request, so these rules are enforced.
4. Copy the Firebase **Web API key** from **Project settings > General**. The backend uses it for Firebase email/password signup, login, and token refresh. Keep it in an environment variable; do not put it in the frontend or commit it.
5. Get a free USDA FoodData Central API key from [api.data.gov](https://api.data.gov/signup/). The server uses it for food autocomplete and nutrition lookups; keep it server-side.

### 2. Build the project

From the repository root, move into `project2` and run the Maven wrapper:

```bash
cd project2
./mvnw clean package
```

### 3. Set Firebase configuration and start Spring Boot

From the `project2` directory, export the Firebase configuration and start Spring Boot:

```bash
export FIREBASE_WEB_API_KEY=your-firebase-web-api-key
export FIREBASE_DATABASE_URL=https://your-database-name.your-region.firebasedatabase.app
export FDC_API_KEY=your-data-gov-api-key

./mvnw spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080) to reach the Welcome page. Choose **Sign up** to create an account with an email address and password, or **Log in** if you already have an account. Ingredients and saved meals persist in Realtime Database under the signed-in user's UID.

### API key and deployment setup

The Firebase keys and USDA API key are read by the Spring Boot server from environment variables; browser JavaScript does not contain them. For GitHub Actions deployment, add `FIREBASE_WEB_API_KEY`, `FIREBASE_DATABASE_URL`, and `FDC_API_KEY` as repository Actions secrets. Keep `GCP_PROJECT_ID`, `GCP_WORKLOAD_IDENTITY_PROVIDER`, and `GCP_DEPLOY_SERVICE_ACCOUNT` as Actions variables. Firestore is not used by this application.

Food search uses USDA FoodData Central generic records (Foundation Foods, FNDDS, and SR Legacy) and branded records. Search results show generic foods first and branded products in a separate group. Search waits 700 ms after typing stops, and repeated query results and food details are cached server-side to reduce API requests. Selecting a result in the pantry imports its nutrition facts; the search phrase remains the pantry ingredient name, while the selected USDA description and FDC ID are saved as nutrition provenance. Meal ingredient fields use the same food autocomplete and save the search phrase, while pantry entries can also be selected to copy their exact name and unit. Pantry matching ignores units and compares ingredient names case-insensitively. Quantity values are compared directly without unit conversion. Quantity and expiry date remain user-entered inventory data. Nutrition facts are saved as a snapshot with their basis and source so later edits do not depend on a live USDA lookup.

## Saved-meal shopping lists

In **Planner → Saved meals**, select a **Cooking date** to check each recipe against the pantry on that day. The date starts at today in the browser. Expired batches are excluded; batches expiring on the selected day and undated batches remain eligible. Changing the date, searching meals, or editing inventory refreshes the results. Each meal is checked independently and no stock is deducted.

`GET /api/meals/{id}/shopping-list?onDate=2026-11-21` returns the existing `needed`, `onHand`, and `toBuy` fields. Omitting `onDate` uses today from the application's clock, consistent with the expiry tracker. Dates must be valid ISO dates within years 0001 to 9999. `onHand` is eligible stock remaining before the current recipe line; earlier lines in the same recipe cannot reuse it. Matching continues to ignore case and units, with no unit conversion. Decimal quantities are calculated without binary subtraction artifacts; unrepresentable pantry totals return HTTP 400.

For example, a recipe needing 3 eggs with 5 expired eggs and 1 eligible egg shows 1 on hand and 2 to buy. An egg batch expiring today is available for today's recipe but excluded when checking tomorrow.

## Weekly meal planning

After saving recipes and pantry ingredients, open **Weekly plan**, choose the start date and priority, and select **Generate weekly plan**. The preview contains one saved recipe per day for seven days and a combined shopping list. Recipes may repeat. Editing meals, ingredients, or planning options clears the previous preview so it can be regenerated from current data.

- **Use food expiring soonest** (`EXPIRY_FIRST`) prioritizes recipes that can use the earliest-expiring remaining stock, then pantry coverage.
- **Prioritize pantry coverage** (`PANTRY_FIRST`) prioritizes the average fulfilled fraction of recipe ingredient lines, then expiry. Ties prefer recipes used fewer times, followed by meal name and ID for deterministic results.

Both strategies allocate each pantry quantity only once across the week and consume earlier-expiring batches first. An ingredient is eligible through its expiration date, but not afterward; undated ingredients remain eligible. Matching uses case-insensitive ingredient names and ignores units. Quantities are compared directly without conversion, so meal and pantry quantities for a matching ingredient should use compatible scales. The planner does not optimize a nutritional target or generate new recipes. Recipes containing only zero quantities are omitted.

The authenticated endpoint is `GET /api/meal-plans/weekly?startDate=2026-11-15&strategy=EXPIRY_FIRST`. The start date is required in ISO `YYYY-MM-DD` format; strategy defaults to `EXPIRY_FIRST`. The response contains `startDate`, `strategy`, `days` (each with `date`, `meal`, and ingredient allocations), and `shoppingList` (only shortfalls, aggregated by ingredient name and unit). Allocations expose `needed`, `fromPantry`, and `toBuy`. No usable saved recipes produces empty lists. Invalid parameters or unrepresentable weekly quantity totals return HTTP 400.

Planning is a read-only preview: it neither saves the plan nor deducts inventory. Ingredient quantities describe one preparation of each saved recipe, not a full daily diet. Tests use the existing in-memory test profile; live Firebase authentication and storage require the configuration described above.
