import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public record ServerData(Map<String, Integer> numbers, Map<Integer, AuctionItem> auctionItems,
                         Map<Integer, List<Bid>> bids, List<User> users,
                         Collection<User> activeUsers) implements Serializable {
}
