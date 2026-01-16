import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class JsonParser {

    private final static List<Slot> slotList = new ArrayList<>();

    public static int parseVenueId(String response) {

        JsonMapper mapper = JsonMapper.builder().build();
        JsonNode root = mapper.readTree(response);
        return root.path("id").path("resy").asInt();

    }

    public static void parseAvailability(String response) {
        JsonMapper mapper = JsonMapper.builder().build();
        JsonNode root = mapper.readTree(response);
        JsonNode slotsNode = root.at("/results/venues/0/slots");

        if (slotsNode.isArray()) {
            for (JsonNode entry : slotsNode) {
                String dateTime = entry.path("date").path("start").asString("");
                int minPartySize = entry.path("size").path("min").asInt(0);
                int maxPartySize = entry.path("size").path("max").asInt(0);
                for (int i = 0; i < (maxPartySize - minPartySize); i++) {
                    Slot newSlot = new Slot(dateTime, minPartySize + i);
                    slotList.add(newSlot);
                }
            }
        }
    }

    public static List<Slot> getSlotList() {
        return slotList;
    }

    public static void clearSlots() {
        slotList.clear();
    }

}
