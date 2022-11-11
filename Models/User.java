import java.io.Serializable;
import java.util.Objects;

public final class User implements Serializable {
    private final String username;
    private final String password;
    private final Constants.ClientType clientType;
    private String email;
    public static final User Empty = new User("", "", null);

    public User(String username, String password, Constants.ClientType clientType) {
        this.username = username;
        this.password = password;
        this.clientType = clientType;
    }

    public void setEmail(String email) {
        this.email = email;
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
