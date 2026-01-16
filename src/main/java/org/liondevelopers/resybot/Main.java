package org.liondevelopers.resybot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

class Main {
    private static final long BASE_MS = TimeUnit.MINUTES.toMillis(5);

    public static void main(String[] args) {

        int partySize = 4;
        String url = "https://resy.com/cities/new-york-ny/venues/cote-nyc?date=2026-01-17&seats=4";
        LocalDate date1 = LocalDate.of(2026,1,16);
        LocalDate date2 = LocalDate.of(2026,1,17);
        List<LocalDate> datesToCheck = new ArrayList<>();
        datesToCheck.add(date1);
        datesToCheck.add(date2);
        LocalTime startTime = LocalTime.of(18,0);
        LocalTime endTime = LocalTime.of(0,00);
        // Scanner scanner = new Scanner(System.in);
        // System.out.print("Enter a Resy restaurant URL (e.g. \"resy.com/cities/new-york-ny/venues/cote-nyc\": ");
        // String url = scanner.nextLine();
        // System.out.print("Enter a party size as an integer: ");
        // String partySizeStr = scanner.nextLine();
        // int partySize = Integer.parseInt(partySizeStr);

        // DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        // List<LocalDate> datesToCheck = new ArrayList<>();

        // System.out.print("Enter a date to search (YYYY-MM-DD) (exit to exit): ");
        // while (true) {
        //     String dateInput = scanner.nextLine();
        //     if (dateInput.equalsIgnoreCase("exit")) {
        //         break;
        //     }
        //     LocalDate date = LocalDate.parse(dateInput, inputFormatter);
        //     datesToCheck.add(date);
        //     System.out.print("Enter another date to search (YYYY-MM-DD) (exit to exit): ");
        // }

        // System.out.print("Enter a start time (HH:mm): ");
        // String startTimeInput = scanner.nextLine();
        // LocalTime startTime = LocalTime.parse(startTimeInput);
        // System.out.print("Enter an end time (HH:mm): ");
        // String endTimeInput = scanner.nextLine();
        // LocalTime endTime = LocalTime.parse(endTimeInput);
        
        // scanner.close();

        ResyApiRequester apiRequester = new ResyApiRequester(url);

        while (true) {
            JsonParser.clearSlots();
            DateTimeFormatter dateTimeSplit = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            apiRequester.getVenueId();
            apiRequester.getWebsiteData(datesToCheck, partySize);
            List<Slot> slotList = JsonParser.getSlotList();
            List<Slot> slotMatches = new ArrayList<>();
            
            for (Slot slot : slotList) {
                LocalDateTime dateTime = LocalDateTime.parse(slot.dateTime, dateTimeSplit);
                boolean isDate = datesToCheck.contains(dateTime.toLocalDate());
                boolean isPartySize = slot.partySize == partySize;
                boolean isTime = isTimeBetween(startTime, endTime, dateTime.toLocalTime());
                if (isDate && isPartySize && isTime) {
                    slotMatches.add(slot);
                }
            }
            
            System.out.println("DEBUG - SLOTLIST: " + slotList);
            System.out.println("DEBUG - SLOTMATCHES: " + slotMatches);
            DiscordNotification.sendMsg(slotMatches);

            if (!slotMatches.isEmpty()) {

                // Notify user that an opening has been found
                // String alertMessage = manager.getFullAvailabilityReport();
                // discordService.sendMessage(alertMessage);
                System.out.println("Opening found! Exiting program.");
                System.exit(0);
            }

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

    public static boolean isTimeBetween(LocalTime start, LocalTime end, LocalTime target) {
        if (end.isBefore(start)) {
            // Over-midnight case: The slot is valid if it's AFTER start OR BEFORE end
            return target.isAfter(start) || target.isBefore(end) || target.equals(start) || target.equals(end);
        }
        // Normal case (e.g., 5 PM to 9 PM)
        return (target.isAfter(start) || target.equals(start))
                && (target.isBefore(end) || target.equals(end));
    }
}
