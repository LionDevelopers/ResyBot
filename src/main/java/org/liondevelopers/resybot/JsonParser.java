import java.util.List;
import java.util.ArrayList;
import tools.jackson.core.JsonNode;
import toold.jackson.core.ObjectMapper;

public class JsonParser {

    private static List<Slot> slotList = new ArrayList<>();

    public static int parseVenueId(String response) {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(response);
        return rootNode.path("id").path("resy").asInt();

    }

    public static void parseAvailability(String response) {

        // for loop going through each entry
        String date;
        /* = json parsed date */
        int partySize;
        /* = json parsed date */
        String time;
        /* = json parsed date */
        Slot slot = new Slot(date, partySize, time);
        slotList.add(slot);

    }

    public static List<Slot> getSlotList() {
        return slotList;
    }

}
