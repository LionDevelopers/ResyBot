public class Slot {
    public String dateTime;
    public int partySize;

    public Slot(String dateTime, int partySize) {

        this.dateTime = dateTime;
        this.partySize = partySize;

    }

    @Override
    public String toString() {
        return String.format("[dateTime: %s | partySize: %d]", 
                             dateTime, partySize);
    }
}