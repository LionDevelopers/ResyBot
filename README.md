## ResyBot to notify and help you reserve seats at your preferred time and date on Resy.

### To-Do List

#### High to Low Priority
*   [X] Add API request
*   [ ] Process API response (strip unnecessary information from JSON?)
*   [ ] Store entire availability list in an object - this allows implementation for other websites (OpenTable)
*   [ ] Check if each object falls within the user's preferred time
*   [ ] Add qualified objects into output list.
*   [ ] Add periodic checking interval for each day. (Check one day, wait x minutes, check next day, etc.) *Limitation is that more days leads to less frequent checking of each individual date.
*   [ ] Add object for each table size e.g. 4-6 adds 3 objects with same date/time but 4, 5, and 6 (loop)
*   [ ] Create notification system (candidates: Discord webhook, email, text). Send output list and exit program

#### Planned Features
*   [ ] Make reservation for user (using account API key)

## How to use:
