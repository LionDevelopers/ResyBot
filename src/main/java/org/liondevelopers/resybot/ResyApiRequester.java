import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResyApiRequester {
    final private String apiKey = "ResyAPI api_key=\"VbWk7s3L4KiK5fzlO7JD3Q5EYolJI7n5\"";
    final private HttpClient client = HttpClient.newHttpClient();
    final private String url;
    private static int venueId;
    final private long baseMs = TimeUnit.MINUTES.toMillis(5);

    public ResyApiRequester(String url) {
        this.url = url;
    }

    public void getVenueId() {
        String city = "NOT_FOUND", slug = "NOT_FOUND";
        Pattern pattern = Pattern.compile("resy\\.com/cities/([^/]+)/(?:venues/)?([^/?]+)");
        Matcher matcher = pattern.matcher(url);

        if (matcher.find()) {
            city = matcher.group(1);
            slug = matcher.group(2);
        }

        String venueUrl = String.format(
                "https://api.resy.com/3/venue?url_slug=%s&location=%s",
                slug, city
        );

        HttpRequest venueRequest = HttpRequest.newBuilder()
                .uri(URI.create(venueUrl))
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0")
                .header("Accept", "application/json")
                .header("Authorization", apiKey)
                .header("Referer", "https://resy.com/")
                .GET()
                .build();

        try {
            // Send the request synchronously and get the response
            HttpResponse<String> response = client.send(venueRequest, HttpResponse.BodyHandlers.ofString());

            // Check the response status code
            if (response.statusCode() == 200) {
                System.out.println("Status Code: " + response.statusCode());
                venueId = JsonParser.parseVenueId(response.body());

            } else {
                System.err.println("API request failed with status code: " + response.statusCode());
                System.err.println("Response body: " + response.body());
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("An error occurred during the HTTP request: " + e.getMessage());
            // e.printStackTrace(); // LOG?
        }
    }

    public void getWebsiteData(List<LocalDate> datesToCheck) {
        for (LocalDate date : datesToCheck) {

            double lat = 0;
            double lon = 0;
            String apiUrl = String.format(
                    "https://api.resy.com/4/find?lat=%f&long=%f&day=%s&venue_id=%s",
                    lat, lon, date, venueId
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0")
                    .header("Accept", "application/json")
                    .header("Authorization", apiKey)
                    .header("Referer", "https://resy.com/")
                    .GET()
                    .build();

            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    System.out.println("Status Code: " + response.statusCode());
                    JsonParser.parseAvailability(response.body());

                } else {
                    System.err.println("API request failed with status code: " + response.statusCode());
                }

            } catch (IOException | InterruptedException e) {
                System.err.println("An error occurred during the HTTP request: " + e.getMessage());
            }
            
            try {
                long jitterMs = ThreadLocalRandom.current().nextLong(0, 1501);
                TimeUnit.MILLISECONDS.sleep(baseMs + jitterMs);
            } catch (InterruptedException e) {
                System.out.println("Sleep was interrupted");
                Thread.currentThread().interrupt();
            }
        }
    }
}