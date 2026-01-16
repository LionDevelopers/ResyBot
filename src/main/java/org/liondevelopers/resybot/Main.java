import java.util.*;

class Main {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter a Resy restaurant URL (e.g. \"resy.com/cities/new-york-ny/venues/cote-nyc\": ");
        String url = scanner.nextLine();
        System.out.print("Enter a party size as an integer: ");
        String partySizeStr = scanner.nextLine();
        int partySize = Integer.parseInt(partySizeStr);
        System.out.print("Enter a date to search (YYYY-MM-DD): ");
        String dateInput = scanner.nextLine();
        scanner.close();

        List<String> dateList = List.of(dateInput);
        AvailabilityManager availList = new AvailabilityManager(dateList);
        Client resyClient = new Client();
        int venueId = resyClient.getVenueId();
        if (venueId == -1) {
            System.out.println("Error fetching venue ID.");
            System.exit(0);
        }

        while (true) {
            boolean somethingNewFound = false;
            String json = resyClient.getWebsiteData(venueId, url, partySize);

            availList.parseJson(json);

            // check if any available time falls between selected time range
            

            // 4. Ping user ONLY if a new slot appeared
            if (somethingNewFound) {
                String alertMessage = manager.getFullAvailabilityReport();
                discordService.sendMessage(alertMessage);
            }

            System.out.println("Check complete. Waiting for next interval...");
            Thread.sleep(60000); // Wait 1 minute before checking all dates again
        }
    }
}
