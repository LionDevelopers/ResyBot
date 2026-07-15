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

    private static final DateTimeFormatter SLOT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("EEE, MMM d 'at' h:mm a");

    /**
     * Sends one enriched embed per newly-found slot. No-op when there are no
     * matches, so it never fires empty payloads on quiet polls.
     */
    public static void sendMsg(String venueName, MonitorRequest req, List<Slot> newMatches) {
        if (newMatches == null || newMatches.isEmpty()) {
            return;
        }

        // Prefer the monitor's own webhook, else fall back to the server-level env webhook.
        String url = req.webhookUrl != null ? req.webhookUrl : Config.get("DISCORD_WEBHOOK");
        if (url == null || url.isEmpty()) {
            System.err.println("Error: no Discord webhook configured (set DISCORD_WEBHOOK or provide one per monitor)!");
            return;
        }

        String title = (venueName == null || venueName.isEmpty()) ? "Reservation available" : venueName;

        try (WebhookClient client = WebhookClient.withUrl(url)) {
            List<WebhookEmbed> embeds = new ArrayList<>();

            for (Slot slot : newMatches) {
                LocalDateTime dateTime = LocalDateTime.parse(slot.dateTime, SLOT_FORMAT);
                LocalDate date = dateTime.toLocalDate();
                String bookingUrl = req.bookingUrl(date);

                String description = String.format(
                        "**%s**%nParty of %d%n%n[Book on Resy](%s)",
                        dateTime.format(DISPLAY_FORMAT), slot.partySize, bookingUrl);

                WebhookEmbed embed = new WebhookEmbedBuilder()
                        .setColor(0x00B14F)
                        .setTitle(new WebhookEmbed.EmbedTitle("🍽️ " + title, bookingUrl))
                        .setDescription(description)
                        .build();
                embeds.add(embed);
            }

            // .join() blocks so the process doesn't close before the send completes.
            client.send(embeds).join();
            System.out.println("Successfully sent " + embeds.size() + " slot(s) to Discord.");
        } catch (Exception e) {
            System.err.println("Failed to send webhook: " + e.getMessage());
        }
    }
}
