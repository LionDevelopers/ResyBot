package org.liondevelopers.resybot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class JsonParser {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    public static int parseVenueId(String response) {
        JsonNode root = MAPPER.readTree(response);
        return root.path("id").path("resy").asInt();
    }

    public static String parseVenueName(String response) {
        JsonNode root = MAPPER.readTree(response);
        return root.path("name").asString("");
    }

    /**
     * @param queryDate the Resy "day" this availability response was requested for; each
     *                  parsed slot records it so late-night slots keep their dining-night
     *                  attribution instead of the calendar date in their timestamp.
     */
    public static List<Slot> parseAvailability(String response, LocalDate queryDate) {
        List<Slot> slots = new ArrayList<>();
        JsonNode root = MAPPER.readTree(response);
        JsonNode slotsNode = root.at("/results/venues/0/slots");

        if (slotsNode.isArray()) {
            for (JsonNode entry : slotsNode) {
                String dateTime = entry.path("date").path("start").asString("");
                int minPartySize = entry.path("size").path("min").asInt(0);
                int maxPartySize = entry.path("size").path("max").asInt(0);
                // Expand the accepted party-size range into one Slot per size so that
                // an exact party-size match (slot.partySize == desired) can be found.
                // Note the inclusive bound: a slot that accepts only min==max would
                // otherwise produce zero Slots and be silently dropped.
                for (int size = minPartySize; size <= maxPartySize; size++) {
                    slots.add(new Slot(dateTime, size, queryDate));
                }
            }
        }
        return slots;
    }
}
