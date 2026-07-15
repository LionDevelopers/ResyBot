package org.liondevelopers.resybot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutable description of a single monitoring request: which Resy venue, which
 * dates, party size, and the acceptable time window. Also derives the city/slug
 * from the Resy URL and can build a booking deep link for notifications.
 */
public class MonitorRequest {

    private static final Pattern URL_PATTERN =
            Pattern.compile("resy\\.com/cities/([^/]+)/(?:venues/)?([^/?]+)");

    public final String url;
    public final List<LocalDate> dates;
    public final int partySize;
    public final LocalTime startTime;
    public final LocalTime endTime;
    public final String city;
    public final String slug;
    /** Optional per-monitor Discord webhook; falls back to the DISCORD_WEBHOOK env when null. */
    public final String webhookUrl;

    public MonitorRequest(String url, List<LocalDate> dates, int partySize,
                          LocalTime startTime, LocalTime endTime) {
        this(url, dates, partySize, startTime, endTime, null);
    }

    public MonitorRequest(String url, List<LocalDate> dates, int partySize,
                          LocalTime startTime, LocalTime endTime, String webhookUrl) {
        this.url = url;
        this.dates = List.copyOf(dates);
        this.partySize = partySize;
        this.startTime = startTime;
        this.endTime = endTime;
        this.webhookUrl = (webhookUrl == null || webhookUrl.isBlank()) ? null : webhookUrl.trim();

        String parsedCity = "NOT_FOUND";
        String parsedSlug = "NOT_FOUND";
        Matcher matcher = URL_PATTERN.matcher(url);
        if (matcher.find()) {
            parsedCity = matcher.group(1);
            parsedSlug = matcher.group(2);
        }
        this.city = parsedCity;
        this.slug = parsedSlug;
    }

    /** One-tap Resy deep link the user can open to book manually. */
    public String bookingUrl(LocalDate date) {
        return String.format(
                "https://resy.com/cities/%s/venues/%s?date=%s&seats=%d",
                city, slug, date, partySize);
    }
}
