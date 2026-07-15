package org.liondevelopers.resybot;

import java.time.LocalDate;

public class Slot {

    public String dateTime;
    public int partySize;
    /**
     * The Resy "day" this slot was fetched under (its dining night). This can differ
     * from the calendar date in {@link #dateTime}: late-night slots (e.g. 12:00 AM) are
     * returned under the previous day's query but carry a timestamp on the next calendar
     * day. Matching and grouping use this so those slots aren't misattributed or dropped.
     */
    public LocalDate queryDate;

    public Slot(String dateTime, int partySize, LocalDate queryDate) {
        this.dateTime = dateTime;
        this.partySize = partySize;
        this.queryDate = queryDate;
    }

    @Override
    public String toString() {
        return String.format("[dateTime: %s | partySize: %d | night: %s]",
                             dateTime, partySize, queryDate);
    }
}
