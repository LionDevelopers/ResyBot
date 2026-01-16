import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

class Main {
    private static final long BASE_MS = TimeUnit.MINUTES.toMillis(5);

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter a Resy restaurant URL (e.g. \"resy.com/cities/new-york-ny/venues/cote-nyc\": ");
        String url = scanner.nextLine();
        System.out.print("Enter a party size as an integer: ");
        String partySizeStr = scanner.nextLine();
        int partySize = Integer.parseInt(partySizeStr);

        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<LocalDate> datesToCheck = new ArrayList<>();
        System.out.print("Enter a date to search (YYYY-MM-DD) (exit to exit): ");
        while (true) {
            String dateInput = scanner.nextLine();
            if (dateInput.equalsIgnoreCase("exit")) {
                break;
            }
            LocalDate date = LocalDate.parse(dateInput, inputFormatter);
            datesToCheck.add(date);
            System.out.print("Enter another date to search (YYYY-MM-DD) (exit to exit): ");
        }

        System.out.print("Enter a start time (HH:mm): ");
        String startTimeInput = scanner.nextLine();
        LocalTime startTime = LocalTime.parse(startTimeInput);
        System.out.print("Enter an end time (HH:mm): ");
        String endTimeInput = scanner.nextLine();
        LocalTime endTime = LocalTime.parse(endTimeInput);
        
        scanner.close();

        ResyApiRequester apiRequester = new ResyApiRequester(url);

        while (true) {
            DateTimeFormatter dateTimeSplit = DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss");
            apiRequester.getWebsiteData(datesToCheck);
            List<Slot> slotList = JsonParser.getSlotList();
            for (Slot slot : slotList) {
                LocalDateTime dateTime = LocalDateTime.parse(slot.dateTime, dateTimeSplit);
                boolean isDate = datesToCheck.contains(dateTime.toLocalDate());
                boolean isPartySize = slot.minPartySize <= partySize && slot.maxPartySize >= partySize;
                boolean isTime = isTimeBetween(startTime, endTime, dateTime.toLocalTime());
                if (isDate && isPartySize && isTime) {

                }
            }

            // check if any available time falls between selected time range


            // 4. Ping user ONLY if a new slot appeared
            // if (/*availability found*/) {
            //     String alertMessage = manager.getFullAvailabilityReport();
            //     discordService.sendMessage(alertMessage);
                
            //     System.exit(0);
            // }

            System.out.println("Check complete. Waiting for next interval...");
            try {
                long jitterMs = ThreadLocalRandom.current().nextLong(0, 1501);
                TimeUnit.MILLISECONDS.sleep(BASE_MS + jitterMs);
            } catch (InterruptedException e) {
                System.out.println("Sleep was interrupted");
                Thread.currentThread().interrupt();
            }
        }
    }

    public static boolean isTimeBetween(LocalTime startTime, LocalTime endTime, LocalTime checkTime) {
        boolean isNotBeforeStart = !checkTime.isBefore(startTime);
        boolean isNotAfterEnd = !checkTime.isAfter(endTime);
        return isNotBeforeStart && isNotAfterEnd;
    }
}
