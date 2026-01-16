package org.liondevelopers.resybot;

import java.util.ArrayList;
import java.util.List;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import io.github.cdimascio.dotenv.Dotenv;

class DiscordNotification {

    public static void sendMsg(List<Slot> slotMatches) {

        String URL = System.getenv("DISCORD_WEBHOOK");
        // Fall back to .env file if not set (for local development)
        if (URL == null || URL.isEmpty()) {
            try {
                Dotenv dotenv = Dotenv.load();
                URL = dotenv.get("DISCORD_WEBHOOK");
            } catch (Exception e) {
                // .env file not found, which is fine if using env vars
            }
        }
        if (URL == null || URL.isEmpty()) {
            System.err.println("Error: DISCORD_WEBHOOK environment variable not set!");
            return;
        }
        
        try (WebhookClient client = WebhookClient.withUrl(URL)) {
            List<WebhookEmbed> embeds = new ArrayList<>();

            for (Slot slot : slotMatches) {
                WebhookEmbed embed = new WebhookEmbedBuilder()
                        .setColor(0xFF0000)
                        .setDescription("Size: " + slot.partySize)
                        .setTitle(new WebhookEmbed.EmbedTitle("Reservation at " + slot.dateTime, null))
                        .build();
                embeds.add(embed);
            }

            // .join() blocks the main thread so the program doesn't close too early
            client.send(embeds).join();
            System.out.println("Successfully sent " + embeds.size() + " slots to Discord.");
        } catch (Exception e) {
            System.err.println("Failed to send webhook: " + e.getMessage());
        }
    }
}