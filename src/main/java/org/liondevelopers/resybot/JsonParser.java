import java.util.List;
import java.util.ArrayList;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class JsonParser {

    private static List<Slot> slotList = new ArrayList<>();

    public static int parseVenueId(String response) {

        JsonMapper mapper = JsonMapper.builder().build();
        JsonNode root = mapper.readTree(response);
        return root.path("id").path("resy").asInt();

    }

    public static void parseAvailability(String response) {
        JsonMapper mapper = JsonMapper.builder().build();
        JsonNode root = mapper.readTree(response);
        JsonNode slotsNode = root.path("slots");

        if (slotsNode.isArray()) {
            for (JsonNode slot : slotsNode) {
                String dateTime = slot.path("date").path("start").asText("");
                int minPartySize = slot.path("size").path("min").asInt(0);
                int maxPartySize = slot.path("size").path("max").asInt(0);
                Slot newSlot = new Slot(dateTime, minPartySize, maxPartySize);
                slotList.add(newSlot);
            }
        }
    }

    public static List<Slot> getSlotList() {
        return slotList;
    }

}
