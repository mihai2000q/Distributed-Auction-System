import java.io.Serializable;

public record LoginResponse(User user, boolean isAuthorized, boolean alreadyLoggedIn, boolean success)
        implements Serializable {
}
