import java.io.Serializable;

public final class ClientRequest implements Serializable {
    private final int id;
    private final User user;

    public ClientRequest(int clientId, User user) {
        super();
        this.id = clientId;
        this.user = user;
    }

    public int getId() {
        return id;
    }

    public User getUser(){
        return user;
    }

    @Override
    public String toString() {
        return "This request" + (user.isEmpty()
                                ? ""
                                : " was made by " +
                                    Normalization.normalizeString(user.getUsername()) + " and it") +
                " got the id: " + id;
    }
}
