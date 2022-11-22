import java.io.Serializable;

public record AuctionItemDto(String itemName, String sellerName, int reservePrice, int startingPrice, String description)
        implements Serializable {
}
