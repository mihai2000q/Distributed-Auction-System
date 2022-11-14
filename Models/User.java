import java.io.Serializable;
import java.util.Objects;

public final class User implements Serializable {
    private final int id;
    private final String username;
    private final String password;
    private final Constants.ClientType clientType;
    private String email;
    public static final User EMPTY = new User(-1,"", "", null);

    public User(int id, String username, String password, Constants.ClientType clientType) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.clientType = clientType;
    }
    public Constants.ClientType getType(){
        return clientType;
    }
    public String getUsername(){
        return username;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public boolean isEmpty(){
        return this == EMPTY;
    }
    @Override
    public String toString() {
        return "The User " + username + " with the id " + id +
                (email == null ? "" : " and the email" + email);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return  username.equals(user.username) &&
                password.equals(user.password) &&
                clientType == user.clientType;
    }
    @Override
    public int hashCode() {
        return Objects.hash(username, password, clientType);
    }
}
