import java.io.Serializable;
import java.util.Objects;

public final class Bid implements Serializable, Comparable<Bid> {
    private final int id;
    private final String itemName;
    private final String username;
    private final int bid;
    public static final Bid EMPTY = new Bid(-1, "N/A", "N/A",-1);

    public Bid(int id, String itemName ,String username, int bid) {
        this.id = id;
        this.itemName = itemName;
        this.username = username;
        this.bid = bid;
    }

    public int getId() {
        return id;
    }

    public String getItemName() {
        return itemName;
    }

    public String getUsername() {
        return username;
    }

    public int getBid() {
        return bid;
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bid bid1 = (Bid) o;
        return id == bid1.id
                && bid == bid1.bid
                && itemName.equals(bid1.itemName)
                && username.equals(bid1.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, itemName, username, bid);
    }

    @Override
    public String toString() {
        return "------------------\n" +
                username + " bid " + Constants.FORMATTER.format(bid) +
                "\n------------------";
    }

    @Override
    public int compareTo(Bid o) {
        return Integer.compare(this.bid, o.bid);
    }
}
