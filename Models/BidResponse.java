import java.io.Serializable;

public record BidResponse(boolean hasItem, int bidComparison) implements Serializable {
}
