## ResyBot to notify and help you reserve seats at your preferred time and date on Resy.

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

#### Planned Features
*   [ ] Make reservation for user (using account API key)

## How to Use

1. Clone the repository to your local machine:
2. Create a .env file based on .env.example and add your Discord Webhook.
3. Run docker compose up --build

#### Known Bugs
*   [ ] Not all time slots are being picked up
*   [ ] 