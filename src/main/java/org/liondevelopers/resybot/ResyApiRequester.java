package org.liondevelopers.resybot;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class ResyApiRequester {

    // Resy's public web-client key (identical for every browser, not a personal secret).
    // Overridable via the RESY_API_KEY env var / .env in case Resy rotates it.
    private static final String DEFAULT_API_KEY = "VbWk7s3L4KiK5fzlO7JD3Q5EYolJI7n5";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0";

    private final String apiKey =
            String.format("ResyAPI api_key=\"%s\"", Config.get("RESY_API_KEY", DEFAULT_API_KEY));
    private final HttpClient client = HttpClient.newHttpClient();
    private final MonitorRequest request;

    private int venueId;
    private String venueName = "";

    // Small delay between per-date requests within a single availability sweep.
    private static final long BETWEEN_DATE_MS = TimeUnit.MILLISECONDS.toMillis(0);

    public ResyApiRequester(MonitorRequest request) {
        this.request = request;
    }

    public int getVenueId() {
        return venueId;
    }

    public String getVenueName() {
        return venueName;
    }

    /**
     * Resolves the numeric venue id and display name from the venue slug/city.
     * Returns true on success.
     */
    public boolean resolveVenue() {
        String venueUrl = String.format(
                "https://api.resy.com/3/venue?url_slug=%s&location=%s",
                request.slug, request.city);

        HttpRequest venueRequest = HttpRequest.newBuilder()
                .uri(URI.create(venueUrl))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .header("Authorization", apiKey)
                .header("Referer", "https://resy.com/")
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(venueRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                venueId = JsonParser.parseVenueId(response.body());
                venueName = JsonParser.parseVenueName(response.body());
                return true;
            }
            System.err.println("Venue API request failed with status code: " + response.statusCode());
            System.err.println("Response body: " + response.body());
        } catch (IOException | InterruptedException e) {
            System.err.println("An error occurred during the venue HTTP request: " + e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        return false;
    }

    /** Fetches and returns all availability slots across the request's dates. */
    public List<Slot> fetchSlots() {
        List<Slot> slots = new ArrayList<>();

        for (LocalDate date : request.dates) {
            System.out.println("Checking reservations for " + date);

            double lat = 0;
            double lon = 0;
            String apiUrl = String.format(
                    "https://api.resy.com/4/find?lat=%f&long=%f&day=%s&party_size=%d&venue_id=%s",
                    lat, lon, date, request.partySize, venueId);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .header("Authorization", apiKey)
                    .header("Referer", "https://resy.com/")
                    .GET()
                    .build();

            try {
                HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    slots.addAll(JsonParser.parseAvailability(response.body()));
                } else {
                    System.err.println("Availability API request failed with status code: " + response.statusCode());
                }
            } catch (IOException | InterruptedException e) {
                System.err.println("An error occurred during the availability HTTP request: " + e.getMessage());
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    return slots;
                }
            }

            try {
                long jitterMs = ThreadLocalRandom.current().nextLong(0, 1501);
                TimeUnit.MILLISECONDS.sleep(BETWEEN_DATE_MS + jitterMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return slots;
            }
        }
        return slots;
    }
}
