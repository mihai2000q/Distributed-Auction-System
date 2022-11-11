import java.io.Serializable;

public final class ClientRequest implements Serializable {
    private final int id;

    public ClientRequest(int clientId) {
        super();
        this.id = clientId;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "This request got the id: " + id;
    }
}
