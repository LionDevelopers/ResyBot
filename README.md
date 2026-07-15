## ResyBot — monitor Resy and get notified the moment a table opens at your preferred date/time.

ResyBot polls Resy's public availability API for a venue you choose and sends an
enriched Discord alert (with a one-tap booking link) as soon as a slot matching your
date, party size, and time window opens up. You start and manage monitors from a small
web UI.

### To-Do List

#### High to Low Priority
*   [X] Add API request
*   [X] Process API response (strip unnecessary information from JSON?)
*   [X] Store entire availability list in an object - this allows implementation for other websites (OpenTable)
*   [X] Check if each object falls within the user's preferred time
*   [X] Add qualified objects into output list.
*   [X] Add periodic checking interval for each day. (Check one day, wait x minutes, check next day, etc.) *Limitation is that more days leads to less frequent checking of each individual date.
*   [X] Add object for each table size e.g. 4-6 adds 3 objects with same date/time but 4, 5, and 6 (loop)
*   [X] Create notification system (candidates: Discord webhook, email, text). Send output list and exit program
*   [X] Enrich Discord notification (venue name + one-tap Resy booking link, only fire on new matches, dedupe)
*   [X] Web UI + REST API to start/stop background monitors (Javalin, multiple concurrent jobs)
*   [X] Make the Resy API key configurable via `RESY_API_KEY`

#### Intentionally Not Building
*   Auto-booking (making the reservation for the user). Resy's TOS prohibits automated
    booking and they actively ban accounts / cancel reservations. ResyBot stays a
    monitor-and-notify tool; the Discord alert includes a booking link so you book
    manually.

## How to Use

1. Clone the repository to your local machine.
2. Create a `.env` file based on `.env.example` and add your Discord Webhook URL.
3. Run `docker compose up --build`.
4. Open `http://localhost:8080` and fill in the form: Resy venue URL, one or more dates,
   party size, and your earliest/latest acceptable time. Submit to start a monitor.
5. Monitors run in the background; you'll get a Discord alert when a matching table opens.
   Manage or stop them from the "Active monitors" list.

### REST API
The web UI is backed by a small JSON API:
* `POST /api/monitors` — `{ "url", "dates": ["yyyy-MM-dd"], "partySize", "startTime": "HH:mm", "endTime": "HH:mm", "webhookUrl"? }`
  (`webhookUrl` is an optional per-monitor Discord webhook; omit it to use the server's `DISCORD_WEBHOOK`)
* `GET /api/monitors` — list active monitors and their status
* `DELETE /api/monitors/{id}` — stop a monitor

#### Known Bugs
*   [X] ~~Not all time slots are being picked up~~ — fixed (off-by-one when a slot's
    min == max party size dropped it entirely).
