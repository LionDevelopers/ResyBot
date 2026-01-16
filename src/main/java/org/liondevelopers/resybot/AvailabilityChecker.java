import java.util.*;

public class AvailabilityChecker {
    private final Map<String, List<String>> availabilityState = new TreeMap<>();

    public AvailabilityChecker(List<String> targetDates) {
        for (String date : targetDates) {
            availabilityState.put(date, new ArrayList<>());
        }
    }

    public void parseJson() {
        for (String date : self.availabilityState) {
            // check date and times for each 
        }
    }

    public boolean updateAndCheckForChanges(String date, List<String> currentSlots) {
        List<String> previousSlots = availabilityState.getOrDefault(date, new ArrayList<>());

        // Check if the current reality matches our memory
        boolean hasChanged = !currentSlots.equals(previousSlots);

        // Update memory for the next loop
        availabilityState.put(date, new ArrayList<>(currentSlots));

        // Only return true if there are actually slots available AND it's a new situation
        return hasChanged && !currentSlots.isEmpty();
    }

    public String getFullAvailabilityReport() {
        StringBuilder report = new StringBuilder("🚨 Resy Alert! 🚨\n");
        availabilityState.forEach((date, slots) -> {
            if (!slots.isEmpty()) {
                report.append(date).append(": ").append(String.join(", ", slots)).append("\n");
            }
        });
        return report.toString();
    }
}