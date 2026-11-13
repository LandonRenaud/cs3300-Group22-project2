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

./mvnw spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080) to reach the Welcome page. Choose **Sign up** to create an account with an email address and password, or **Log in** if you already have an account. Ingredients and saved meals persist in Realtime Database under the signed-in user's UID.

### API key and deployment setup

The Firebase Web API key is read by the Spring Boot server from `FIREBASE_WEB_API_KEY`; browser JavaScript does not contain it. For the GitHub Actions deployment, add `FIREBASE_WEB_API_KEY` as a repository Actions secret, set `FIREBASE_DATABASE_URL` as a repository Actions variable, and set the existing `GCP_PROJECT_ID` Actions variable to the Google Cloud project ID used for deployment. Firestore is not used by this application.
