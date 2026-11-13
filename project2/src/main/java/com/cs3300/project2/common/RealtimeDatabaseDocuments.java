package com.cs3300.project2.common;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Firebase Realtime Database REST access authenticated as the current Firebase user. */
@Component
@Profile("!test")
public class RealtimeDatabaseDocuments {
    private final FirebaseSettings settings;
    private final JsonMapper mapper;
    private final HttpClient http;

    public RealtimeDatabaseDocuments(FirebaseSettings settings, JsonMapper mapper, HttpClient http) {
        this.settings = settings;
        this.mapper = mapper;
        this.http = http;
    }

    public <T> List<T> list(String collection, Class<T> type) {
        JsonNode values = exchange("GET", collectionUrl(collection), null);
        List<T> records = new ArrayList<>();
        if (values != null && values.isObject()) {
            values.properties().forEach(entry -> records.add(mapper.treeToValue(entry.getValue(), type)));
        }
        return records;
    }

    public <T> Optional<T> find(String collection, String id, Class<T> type) {
        JsonNode value = exchange("GET", documentUrl(collection, id), null);
        return value == null || value.isNull() ? Optional.empty() : Optional.of(mapper.treeToValue(value, type));
    }

    public <T> T save(String collection, String id, T value, Class<T> type) {
        JsonNode payload = mapper.valueToTree(value);
        JsonNode saved = exchange("PUT", documentUrl(collection, id), payload);
        return mapper.treeToValue(saved == null ? payload : saved, type);
    }

    public boolean delete(String collection, String id) {
        JsonNode existing = exchange("GET", documentUrl(collection, id), null);
        if (existing == null || existing.isNull()) return false;
        exchange("DELETE", documentUrl(collection, id), null);
        return true;
    }

    private String collectionUrl(String collection) {
        return baseUrl() + "/users/" + pathPart(UserContext.requireUid()) + "/" + pathPart(collection) + ".json";
    }

    private String documentUrl(String collection, String id) {
        return baseUrl() + "/users/" + pathPart(UserContext.requireUid()) + "/" + pathPart(collection)
                + "/" + pathPart(id) + ".json";
    }

    private String baseUrl() {
        return settings.databaseUrl().replaceAll("/+$", "");
    }

    private static String pathPart(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private JsonNode exchange(String method, String endpoint, JsonNode payload) {
        String url = endpoint + "?auth=" + queryPart(UserContext.requireIdToken());
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .header("Accept", "application/json");
            if (payload == null) {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                builder.header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)));
            }
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode body = response.body().isBlank() ? null : mapper.readTree(response.body());
            if (response.statusCode() >= 400) {
                String detail = body == null ? response.body() : body.asText(response.body());
                throw new IllegalStateException("Realtime Database REST request failed (HTTP "
                        + response.statusCode() + "): " + detail);
            }
            return body;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Realtime Database request was interrupted", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not reach the Firebase Realtime Database", ex);
        }
    }

    private static String queryPart(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
