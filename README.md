## ResyBot: monitor Resy and get notified the moment a table opens at your preferred date/time.

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

### Prerequisites

* Docker and Docker Compose (the simplest path), or Java 17+ and Maven if you prefer to
  run it directly.
* A Discord webhook URL. In Discord, open a server you manage, go to Server Settings >
  Integrations > Webhooks, create a webhook, choose the channel it should post to, and
  copy its URL.

### Setup

1. Clone the repository and change into it:

   ```
   git clone <repo-url>
   cd ResyBot
   ```

2. Create your environment file from the example and open it:

   ```
   cp .env.example .env
   ```

   Set `DISCORD_WEBHOOK` to the webhook URL you copied above. `RESY_API_KEY` and `PORT`
   are optional; leave them commented out to use the defaults (the built-in public Resy
   key and port 8080).

### Run

Using Docker (recommended):

```
docker compose up --build
```

Or run it directly with Maven:

```
./mvnw package
java -jar target/resybot-1.0-SNAPSHOT.jar
```

Either way, the server starts on `http://localhost:8080`.

### Start a monitor

1. Open `http://localhost:8080` in your browser.
2. Fill in the form:
   * **Resy venue URL**: the venue page from resy.com, for example
     `https://resy.com/cities/new-york-ny/venues/cote-nyc`.
   * **Dates**: one or more dates to watch. Use "Add date" for more than one.
   * **Party size**: the number of seats you need.
   * **Earliest time** and **Latest time**: the window you would accept. Windows that
     cross midnight (for example 22:00 to 01:00) are supported.
   * **Discord webhook** (optional): a webhook just for this monitor. Leave it blank to
     use the server's default `DISCORD_WEBHOOK`.
3. Click "Start monitoring". The monitor appears under "Active monitors" with its live
   status and the time of its last check.

### What happens next

Each monitor polls Resy roughly every five minutes in the background. When a slot that
matches your date, party size, and time window opens up, you get a Discord message with
the venue name, the date and time, and a one-tap link to book it on Resy. ResyBot does
not book for you; you complete the reservation yourself from that link. The same slot is
only alerted once, so you will not be spammed on later checks.

### Stop a monitor

Click "Stop" on any monitor in the "Active monitors" list. You can run several monitors
at once, and they persist until you stop them or the server shuts down.

### REST API
The web UI is backed by a small JSON API:
* `POST /api/monitors`: `{ "url", "dates": ["yyyy-MM-dd"], "partySize", "startTime": "HH:mm", "endTime": "HH:mm", "webhookUrl"? }`
  (`webhookUrl` is an optional per-monitor Discord webhook; omit it to use the server's `DISCORD_WEBHOOK`)
* `GET /api/monitors`: list active monitors and their status
* `DELETE /api/monitors/{id}`: stop a monitor

#### Known Bugs
*   [X] ~~Not all time slots are being picked up~~ (fixed: off-by-one when a slot's
    min == max party size dropped it entirely).
