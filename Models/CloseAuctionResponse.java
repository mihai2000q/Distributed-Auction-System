import java.io.Serializable;

public record CloseAuctionResponse(boolean hasItem, boolean isAuthorized, String winner) implements Serializable {
}
