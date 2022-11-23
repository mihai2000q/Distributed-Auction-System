import java.io.Serializable;

public record InfoResponse(boolean hasItem, boolean isAuthorized, boolean ongoing, boolean isWinner)
        implements Serializable {
}
