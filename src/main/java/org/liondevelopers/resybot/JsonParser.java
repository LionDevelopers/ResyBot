
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpResponse;

public class JsonParser {

    public static int parseVenueId(String response) {

        System.out.println("Status Code: " + response.statusCode());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(response.body());
        int venueId = rootNode.path("id").path("resy").asInt();
        return venueId;

    }

    public static void parseWebsiteData(String response) {



    }
}
