package org.liondevelopers.resybot;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * A single background monitoring job: repeatedly polls Resy availability for one
 * {@link MonitorRequest}, notifies Discord when a matching slot first appears, and
 * dedupes so the same slot is not re-alerted on later polls. A slot is only marked as
 * notified once its Discord send actually succeeds, so a failed or partial send (e.g.
 * more matches than fit in one Discord message) gets retried on the next poll instead
 * of being silently dropped. Runs until {@link #stop()}.
 */
public class MonitorJob implements Runnable {

    private static final long BASE_MS = TimeUnit.MINUTES.toMillis(5);
    private static final DateTimeFormatter SLOT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String id;
    private final MonitorRequest request;
    private final ResyApiRequester api;
    private final Set<String> notified = ConcurrentHashMap.newKeySet();

    private volatile boolean running = true;
    private volatile String status = "starting";
    private volatile long lastCheckedEpochMs = 0;

    public MonitorJob(String id, MonitorRequest request) {
        this.id = id;
        this.request = request;
        this.api = new ResyApiRequester(request);
    }

    public String getId() {
        return id;
    }

    public MonitorRequest getRequest() {
        return request;
    }

    public String getStatus() {
        return status;
    }

    public long getLastCheckedEpochMs() {
        return lastCheckedEpochMs;
    }

    public String getVenueName() {
        return api.getVenueName();
    }

    public void stop() {
        running = false;
        status = "stopped";
    }

    @Override
    public void run() {
        if (!api.resolveVenue()) {
            status = "error: could not resolve venue";
            System.err.println("[" + id + "] Could not resolve venue for " + request.url);
            return;
        }
        status = "monitoring";
        System.out.println("[" + id + "] Monitoring " + api.getVenueName() + " (venueId=" + api.getVenueId() + ")");

        while (running && !Thread.currentThread().isInterrupted()) {
            List<Slot> slots = api.fetchSlots();
            lastCheckedEpochMs = System.currentTimeMillis();

            // Filter to slots not yet notified, but don't mark them notified until the
            // send actually succeeds — otherwise a failed/partial Discord send (e.g. more
            // than 10 matches in one poll) would permanently suppress those slots.
            List<Slot> candidates = new ArrayList<>();
            for (Slot slot : slots) {
                if (matches(slot) && !notified.contains(dedupeKey(slot))) {
                    candidates.add(slot);
                }
            }

            if (!candidates.isEmpty()) {
                System.out.println("[" + id + "] Found " + candidates.size() + " new matching slot(s): " + candidates);
                List<Slot> sent = DiscordNotification.sendMsg(api.getVenueName(), request, candidates);
                for (Slot slot : sent) {
                    notified.add(dedupeKey(slot));
                }
                if (sent.size() < candidates.size()) {
                    System.err.println("[" + id + "] " + (candidates.size() - sent.size())
                            + " slot(s) failed to notify and will be retried next poll.");
                }
            } else {
                System.out.println("[" + id + "] No new matches. Waiting for next interval...");
            }

            if (!sleepInterval()) {
                break;
            }
        }
        status = "stopped";
    }

    private boolean matches(Slot slot) {
        LocalDateTime dateTime = LocalDateTime.parse(slot.dateTime, SLOT_FORMAT);
        boolean isDate = request.dates.contains(dateTime.toLocalDate());
        boolean isPartySize = slot.partySize == request.partySize;
        boolean isTime = isTimeBetween(request.startTime, request.endTime, dateTime.toLocalTime());
        return isDate && isPartySize && isTime;
    }

    private String dedupeKey(Slot slot) {
        return slot.dateTime + "|" + slot.partySize;
    }

    /** Sleeps BASE_MS + jitter; returns false if interrupted. */
    private boolean sleepInterval() {
        try {
            long jitterMs = ThreadLocalRandom.current().nextLong(0, 1501);
            TimeUnit.MILLISECONDS.sleep(BASE_MS + jitterMs);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public static boolean isTimeBetween(LocalTime start, LocalTime end, LocalTime target) {
        if (end.isBefore(start)) {
            // Over-midnight window: valid if after start OR before end (inclusive of bounds).
            return target.isAfter(start) || target.isBefore(end)
                    || target.equals(start) || target.equals(end);
        }
        return (target.isAfter(start) || target.equals(start))
                && (target.isBefore(end) || target.equals(end));
    }
}
