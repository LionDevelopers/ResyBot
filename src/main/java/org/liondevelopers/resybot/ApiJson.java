package org.liondevelopers.resybot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * JSON (de)serialization for the REST API, using the project's existing
 * tools.jackson mapper so we don't depend on Javalin's bundled Jackson.
 */
public final class ApiJson {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private ApiJson() {
    }

    /**
     * Parses a monitor request body of the form:
     * {@code {"url": "...", "dates": ["2026-01-16", ...], "partySize": 4,
     *         "startTime": "18:00", "endTime": "00:00"}}
     *
     * @throws IllegalArgumentException if required fields are missing/invalid
     */
    public static MonitorRequest parseMonitorRequest(String body) {
        JsonNode root;
        try {
            root = MAPPER.readTree(body == null ? "" : body);
        } catch (Exception e) {
            throw new IllegalArgumentException("Body is not valid JSON");
        }
        if (root == null || root.isMissingNode() || !root.isObject()) {
            throw new IllegalArgumentException("Expected a JSON object");
        }

        String url = text(root, "url");
        if (url.isEmpty()) {
            throw new IllegalArgumentException("'url' is required");
        }

        JsonNode datesNode = root.path("dates");
        if (!datesNode.isArray() || datesNode.isEmpty()) {
            throw new IllegalArgumentException("'dates' must be a non-empty array of yyyy-MM-dd strings");
        }
        List<LocalDate> dates = new ArrayList<>();
        for (JsonNode d : datesNode) {
            try {
                dates.add(LocalDate.parse(d.asString("")));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid date: " + d.asString("") + " (expected yyyy-MM-dd)");
            }
        }

        int partySize = root.path("partySize").asInt(0);
        if (partySize <= 0) {
            throw new IllegalArgumentException("'partySize' must be a positive integer");
        }

        LocalTime startTime = parseTime(text(root, "startTime"), "startTime");
        LocalTime endTime = parseTime(text(root, "endTime"), "endTime");

        String webhookUrl = text(root, "webhookUrl");
        if (!webhookUrl.isEmpty() && !webhookUrl.startsWith("https://discord.com/api/webhooks/")
                && !webhookUrl.startsWith("https://discordapp.com/api/webhooks/")) {
            throw new IllegalArgumentException("'webhookUrl' must be a Discord webhook URL");
        }

        return new MonitorRequest(url, dates, partySize, startTime, endTime,
                webhookUrl.isEmpty() ? null : webhookUrl);
    }

    public static String jobToJson(MonitorJob job) {
        return MAPPER.writeValueAsString(jobNode(job));
    }

    public static String jobsToJson(Collection<MonitorJob> jobs) {
        ArrayNode arr = MAPPER.createArrayNode();
        for (MonitorJob job : jobs) {
            arr.add(jobNode(job));
        }
        return MAPPER.writeValueAsString(arr);
    }

    public static String error(String message) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("error", message);
        return MAPPER.writeValueAsString(node);
    }

    private static ObjectNode jobNode(MonitorJob job) {
        MonitorRequest req = job.getRequest();
        ObjectNode node = MAPPER.createObjectNode();
        node.put("id", job.getId());
        node.put("venueName", job.getVenueName());
        node.put("status", job.getStatus());
        node.put("url", req.url);
        node.put("partySize", req.partySize);
        node.put("startTime", req.startTime.toString());
        node.put("endTime", req.endTime.toString());
        node.put("lastCheckedEpochMs", job.getLastCheckedEpochMs());
        // Expose only whether a per-monitor webhook is set — never echo the URL itself.
        node.put("hasWebhookOverride", req.webhookUrl != null);
        ArrayNode dates = node.putArray("dates");
        req.dates.forEach(d -> dates.add(d.toString()));
        return node;
    }

    private static String text(JsonNode root, String field) {
        return root.path(field).asString("").trim();
    }

    private static LocalTime parseTime(String value, String field) {
        if (value.isEmpty()) {
            throw new IllegalArgumentException("'" + field + "' is required (HH:mm)");
        }
        try {
            return LocalTime.parse(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid '" + field + "': " + value + " (expected HH:mm)");
        }
    }
}
