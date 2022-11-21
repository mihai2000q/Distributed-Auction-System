import java.io.Serializable;

public record CloseAuctionResponse(boolean hasItem, boolean isAuthorized, boolean isClosedAlready, String winner)
        implements Serializable {
}
