package org.liondevelopers.resybot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;

class DiscordNotification {

    // Discord rejects webhook messages with more than 10 embeds, so batches larger
    // than this must be split into multiple sends.
    private static final int MAX_EMBEDS_PER_MESSAGE = 10;

    private static final DateTimeFormatter SLOT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEE, MMM d");
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm a");

    /**
     * Sends one embed per dining night, each listing all newly-found times for that
     * night. Grouping matters: Discord collapses multiple embeds in a message that share
     * the same title URL, so one embed per time (which all share the same booking URL for
     * a given date/party) would render as a single embed and silently hide the rest.
     * Embeds are chunked into Discord's 10-per-message limit. No-op when there are no
     * matches.
     *
     * @return the subset of {@code newMatches} that were successfully delivered. Slots
     *         whose batch failed to send are omitted so the caller can retry them on a
     *         later poll instead of marking them as notified.
     */
    public static List<Slot> sendMsg(String venueName, MonitorRequest req, List<Slot> newMatches) {
        if (newMatches == null || newMatches.isEmpty()) {
            return List.of();
        }

        // Prefer the monitor's own webhook, else fall back to the server-level env webhook.
        String url = req.webhookUrl != null ? req.webhookUrl : Config.get("DISCORD_WEBHOOK");
        if (url == null || url.isEmpty()) {
            System.err.println("Error: no Discord webhook configured (set DISCORD_WEBHOOK or provide one per monitor)!");
            return List.of();
        }

        String title = (venueName == null || venueName.isEmpty()) ? "Reservation available" : venueName;

        // Group by dining night, preserving first-seen order, and sort each night's times.
        Map<LocalDate, List<Slot>> byNight = new LinkedHashMap<>();
        for (Slot slot : newMatches) {
            byNight.computeIfAbsent(slot.queryDate, k -> new ArrayList<>()).add(slot);
        }
        for (List<Slot> group : byNight.values()) {
            group.sort(Comparator.comparing(s -> LocalDateTime.parse(s.dateTime, SLOT_FORMAT)));
        }
        List<Map.Entry<LocalDate, List<Slot>>> nights = new ArrayList<>(byNight.entrySet());

        List<Slot> sent = new ArrayList<>();
        try (WebhookClient client = WebhookClient.withUrl(url)) {
            for (int i = 0; i < nights.size(); i += MAX_EMBEDS_PER_MESSAGE) {
                List<Map.Entry<LocalDate, List<Slot>>> batch =
                        nights.subList(i, Math.min(i + MAX_EMBEDS_PER_MESSAGE, nights.size()));

                List<WebhookEmbed> embeds = new ArrayList<>();
                List<Slot> batchSlots = new ArrayList<>();
                for (Map.Entry<LocalDate, List<Slot>> night : batch) {
                    embeds.add(buildEmbed(title, req, night.getKey(), night.getValue()));
                    batchSlots.addAll(night.getValue());
                }

                try {
                    // .join() blocks so the process doesn't close before the send completes.
                    client.send(embeds).join();
                    sent.addAll(batchSlots);
                } catch (Exception e) {
                    System.err.println("Failed to send Discord batch of " + batchSlots.size()
                            + " slot(s), will retry next poll: " + e.getMessage());
                }
            }
            System.out.println("Successfully sent " + sent.size() + "/" + newMatches.size() + " slot(s) to Discord.");
        } catch (Exception e) {
            System.err.println("Failed to send webhook: " + e.getMessage());
        }
        return sent;
    }

    private static WebhookEmbed buildEmbed(String title, MonitorRequest req, LocalDate night, List<Slot> slots) {
        String bookingUrl = req.bookingUrl(night);
        int partySize = slots.get(0).partySize;

        StringBuilder times = new StringBuilder();
        for (Slot slot : slots) {
            LocalDateTime dateTime = LocalDateTime.parse(slot.dateTime, SLOT_FORMAT);
            times.append("• ").append(dateTime.format(TIME_FORMAT)).append('\n');
        }

        String description = String.format(
                "**%s** · Party of %d%n%s%n[Book on Resy](%s)",
                night.format(DATE_FORMAT), partySize, times, bookingUrl);

        return new WebhookEmbedBuilder()
                .setColor(0x00B14F)
                .setTitle(new WebhookEmbed.EmbedTitle("🍽️ " + title, bookingUrl))
                .setDescription(description)
                .build();
    }
}
