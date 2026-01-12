
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class Monitor {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter a Resy restaurant URL (e.g. \"resy.com/cities/new-york-ny/venues/cote-nyc\": ");
        String url = scanner.nextLine();
        scanner.close();
        // Add URL validation?

        // 
        Pattern pattern = Pattern.compile("resy\\.com/cities/([^/]+)/(?:venues/)?([^/?]+)");
        Matcher matcher = pattern.matcher(url);

        String city = "NOT_FOUND", slug = "NOT_FOUND";

        if (matcher.find()) {
            city = matcher.group(1);
            slug = matcher.group(2);
        }

        System.out.println("DEBUG - City: " + city);
        System.out.println("DEBUG - Slug: " + slug);

        // Define the URL of the API endpoint
        double lat = 0;
        double lon = 0;
        String date = "2026-01-13";
        String partySize = "4";
        String venueId = "60058";
        String apiUrl = String.format(
                "https://api.resy.com/4/find?lat=%f&long=%f&day=%s&party_size=%s&venue_id=%s",
                lat, lon, date, partySize, venueId
        );

        String venueUrl = String.format(
                "https://api.resy.com/3/venue?url_slug=%s&location=%s",
                slug, city
        );

        System.out.println("Venue URL: " + venueUrl);

        // Build the HttpRequest
        String apiKey = "ResyAPI api_key=\"VbWk7s3L4KiK5fzlO7JD3Q5EYolJI7n5\"";

        // Create an HttpClient instance
        HttpClient client = HttpClient.newHttpClient();

        // Make request for restaurant venue ID
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
                // String jsonBody = response.body();
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(response.body());
                int resyVenueId = rootNode.path("id").path("resy").asInt();
                System.out.println("DEBUG - Venue ID: " + resyVenueId);
                // String fileName = "venueResponse.json";
                // Files.writeString(Paths.get(fileName), jsonBody);
                // System.out.println("Response saved to " + fileName);

            } else {
                System.err.println("API request failed with status code: " + response.statusCode());
                System.err.println("Response body: " + response.body());
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("An error occurred during the HTTP request: " + e.getMessage());
            e.printStackTrace();
        }

        // // Make request for reservation availabilities
        // HttpRequest request = HttpRequest.newBuilder()
        //         .uri(URI.create(apiUrl))
        //         .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0")
        //         .header("Accept", "application/json")
        //         .header("Authorization", apiKey)
        //         .header("Referer", "https://resy.com/")
        //         .GET()
        //         .build();

        // try {
        //     // Send the request synchronously and get the response
        //     HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        //     // Check the response status code
        //     if (response.statusCode() == 200) {
        //         // Print the raw JSON response body
        //         System.out.println("Status Code: " + response.statusCode());
        //         System.out.println("Response Body (JSON):");
        //         System.out.println(response.body());

        //         // You can then use a JSON library (like Gson) to parse response.body()
        //         // into Java objects (see the "Parsing the JSON Response" section below)
        //     } else {
        //         System.err.println("API request failed with status code: " + response.statusCode());
        //         System.err.println("Response body: " + response.body());
        //     }

        // } catch (IOException | InterruptedException e) {
        //     System.err.println("An error occurred during the HTTP request: " + e.getMessage());
        //     e.printStackTrace();
        // }
    }
}
