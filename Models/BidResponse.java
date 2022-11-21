import java.io.Serializable;

public record BidResponse(boolean hasItem, int startingPriceComparison, int bidComparison) implements Serializable {
    public static final BidResponse EMPTY = new BidResponse(false, -2, -2);
    public boolean isEmpty(){
        return this == EMPTY;
    }
}
