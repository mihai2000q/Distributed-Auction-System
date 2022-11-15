import java.io.Serializable;
import java.util.Objects;

public final class Bid implements Serializable {
    private final int id;
    private final String username;
    private final int bid;
    public static final Bid EMPTY = new Bid(-1, "N/A", -1);

    public Bid(int id, String username, int bid) {
        this.id = id;
        this.username = username;
        this.bid = bid;
    }

    public String getUsername() {
        return username;
    }

    public int getId() {
        return id;
    }

    public int getBid() {
        return bid;
    }

    public boolean isEmpty(){
        return this == EMPTY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bid bid1 = (Bid) o;
        return  id == bid1.id &&
                bid == bid1.bid &&
                username.equals(bid1.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, bid);
    }
}
