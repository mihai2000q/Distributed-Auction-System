import java.io.Serializable;

public record BidRequest(int auctionId, int bid) implements Serializable {
}
