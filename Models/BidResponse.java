import java.io.Serializable;

public record BidResponse(boolean hasItem, boolean isLowerThanReservedPrice , boolean ongoing, int bidComparison)
        implements Serializable {
}
