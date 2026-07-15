package org.liondevelopers.resybot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;

class DiscordNotification {

    // Discord rejects webhook messages with more than 10 embeds, so batches larger
    // than this must be split into multiple sends.
    private static final int MAX_EMBEDS_PER_MESSAGE = 10;

    private static final DateTimeFormatter SLOT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("EEE, MMM d 'at' h:mm a");

    /**
     * Sends one enriched embed per newly-found slot, chunked into Discord's 10-embed-
     * per-message limit. No-op when there are no matches, so it never fires empty
     * payloads on quiet polls.
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
        List<Slot> sent = new ArrayList<>();

        try (WebhookClient client = WebhookClient.withUrl(url)) {
            for (int i = 0; i < newMatches.size(); i += MAX_EMBEDS_PER_MESSAGE) {
                List<Slot> batch = newMatches.subList(i, Math.min(i + MAX_EMBEDS_PER_MESSAGE, newMatches.size()));
                List<WebhookEmbed> embeds = new ArrayList<>();
                for (Slot slot : batch) {
                    embeds.add(buildEmbed(title, req, slot));
                }
                try {
                    // .join() blocks so the process doesn't close before the send completes.
                    client.send(embeds).join();
                    sent.addAll(batch);
                } catch (Exception e) {
                    System.err.println("Failed to send Discord batch of " + batch.size()
                            + " slot(s), will retry next poll: " + e.getMessage());
                }
            }
            System.out.println("Successfully sent " + sent.size() + "/" + newMatches.size() + " slot(s) to Discord.");
        } catch (Exception e) {
            System.err.println("Failed to send webhook: " + e.getMessage());
        }
        return sent;
    }

    private static WebhookEmbed buildEmbed(String title, MonitorRequest req, Slot slot) {
        LocalDateTime dateTime = LocalDateTime.parse(slot.dateTime, SLOT_FORMAT);
        LocalDate date = dateTime.toLocalDate();
        String bookingUrl = req.bookingUrl(date);

        String description = String.format(
                "**%s**%nParty of %d%n%n[Book on Resy](%s)",
                dateTime.format(DISPLAY_FORMAT), slot.partySize, bookingUrl);

        return new WebhookEmbedBuilder()
                .setColor(0x00B14F)
                .setTitle(new WebhookEmbed.EmbedTitle("🍽️ " + title, bookingUrl))
                .setDescription(description)
                .build();
    }
}
