package com.deploysync.model.weblogic;

import com.deploysync.model.eardeployment.DeploymentResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Service
public class WeblogicService {
    private static final int MAX_ERROR_LENGTH = 300;
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration REDEPLOY_TIMEOUT = Duration.ofMinutes(5);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(DEFAULT_TIMEOUT)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DeploymentResult stop(WeblogicConnection connection) {
        return summarize(post(connection, "stop", "{}"), connection, "Stop");
    }

    public DeploymentResult start(WeblogicConnection connection) {
        return summarize(post(connection, "start", "{}"), connection, "Start");
    }

    public DeploymentResult redeploy(WeblogicConnection connection) {
        return summarize(post(connection, "redeploy", "{}"), connection, "Redeploy");
    }

    public DeploymentResult checkState(WeblogicConnection connection) {
        DeploymentResult raw = post(connection, "getState", "{\"target\":\"" + connection.target() + "\"}");
        if (!raw.success()) {
            return raw;
        }

        try {
            JsonNode node = objectMapper.readTree(raw.message());
            String state = node.path("return").asText("UNKNOWN");
            return new DeploymentResult(true, connection.appName() + " on " + connection.target() + " is " + state);
        } catch (IOException e) {
            return new DeploymentResult(false, "Unexpected response: " + raw.message());
        }
    }

    private DeploymentResult summarize(DeploymentResult raw, WeblogicConnection connection, String actionLabel) {
        if (!raw.success()) {
            return raw;
        }
        return new DeploymentResult(true,
                connection.appName() + " " + actionLabel.toLowerCase() + " request accepted on " + connection.target() + ".");
    }

    private DeploymentResult post(WeblogicConnection connection, String action, String bodyJson) {
        String uri = connection.baseUrl()
                + "/domainRuntime/deploymentManager/appDeploymentRuntimes/"
                + connection.appName()
                + "/" + action;

        String credentials = connection.username() + ":" + connection.password();
        String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .timeout(REDEPLOY_TIMEOUT)
                .header("Authorization", "Basic " + basicAuth)
                .header("X-Requested-By", "automation")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                return new DeploymentResult(false, "HTTP " + response.statusCode() + ": " + summarizeError(response.body()));
            }
            return new DeploymentResult(true, response.body());
        } catch (IOException e) {
            return new DeploymentResult(false, "Request failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new DeploymentResult(false, "Request interrupted.");
        }
    }

    private String summarizeError(String body) {
        if (body == null || body.isBlank()) {
            return "(no response body)";
        }

        try {
            JsonNode node = objectMapper.readTree(body);
            String detail = node.path("detail").asText(null);
            if (detail == null) {
                detail = node.path("message").asText(null);
            }
            if (detail != null && !detail.isBlank()) {
                return truncate(detail);
            }
        } catch (IOException e) {
            // not JSON - fall through to raw truncation below
        }

        return truncate(body);
    }

    private String truncate(String text) {
        String trimmed = text.strip();
        return trimmed.length() > MAX_ERROR_LENGTH
                ? trimmed.substring(0, MAX_ERROR_LENGTH) + "..."
                : trimmed;
    }
}
