import java.io.Serializable;

public record BidResponse(boolean hasItem, int startingPriceComparison, int bidComparison) implements Serializable {
}
