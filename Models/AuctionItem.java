import java.io.Serializable;
import java.util.Objects;

public final class AuctionItem implements Serializable, Comparable<AuctionItem> {
    private final int auctionId;
    private final int id;
    private final String itemName;
    private final String sellerName;
    private final int startingPrice;
    private final int reservePrice;
    private final String description;
    private String highestBidName = "None";
    private int currentBid = 0;
    public static final AuctionItem EMPTY = new AuctionItem(-1, -1, "N/A", "N/A",
                                                        -1, -1, "N/A");

    public AuctionItem(int id, int auctionId, String itemName, String sellerName,
                       int startingPrice, int reservePrice, String description) {
        this.id = id;
        this.auctionId = auctionId;
        this.itemName = itemName;
        this.sellerName = sellerName;
        this.startingPrice = startingPrice;
        this.reservePrice = reservePrice;
        this.description = description;
    }

    public AuctionItem setNewId(int id) {
        return new AuctionItem(id, this.auctionId, this.itemName, this.sellerName,
                this.startingPrice, this.reservePrice, this.description);
    }

    public AuctionItem setAuctionId(int auctionId) {
        return new AuctionItem(id, auctionId, this.itemName, this.sellerName,
                this.startingPrice, this.reservePrice, this.description);
    }

    public String getItemName() {
        return itemName;
    }

    public int getId() {
        return id;
    }

    public void setNewBid(String bidderName, int bid) {
        currentBid = bid;
        highestBidName = bidderName;
    }

    public int getStartingPrice() {
        return startingPrice;
    }

    public String getSellerName() {
        return Normalization.normalizeString(sellerName);
    }

    public String getHighestBidName(){
        return Normalization.normalizeString(highestBidName);
    }

    public int getCurrentBid() {
        return currentBid;
    }

    public int getReservePrice() {
        return reservePrice;
    }

    public boolean isBid() {
        return !(currentBid == 0 && highestBidName.equals("None"));
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuctionItem item = (AuctionItem) o;
        return  id == item.id && startingPrice == item.startingPrice &&
                reservePrice == item.reservePrice &&
                Objects.equals(itemName, item.itemName) &&
                Objects.equals(sellerName, item.sellerName) &&
                Objects.equals(description, item.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, itemName, sellerName, startingPrice, reservePrice, description);
    }

    @Override
    public String toString() {
        return  "The auction id is:\t " + auctionId +
                "\n-----------------------------\n" +
                "Seller name = " + getSellerName() +
                "\nItem name = " + itemName + "\nStarting Price = " + startingPrice +
                "\nCurrent bid = " + currentBid +
                "\nDescription = " + description +
                "\n-----------------------------\n";
    }

    @Override
    public int compareTo(AuctionItem o) {
        return Integer.compare(currentBid, o.getCurrentBid());
    }
}