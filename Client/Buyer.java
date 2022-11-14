import java.io.IOException;
import java.rmi.RemoteException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public final class Buyer extends Client {
    public Buyer() {
        super();
    }
    public static void main(String[] args) {
        var response = connectToServer(Constants.ClientType.Buyer);
        if(response == null)
            return;
        final IBuyer server = (IBuyer) response.x();
        final User user = response.y();
        int answer;
        Scanner scanner = new Scanner(System.in);
        do {
            System.out.println("""
                           
                            Please type
                            
                            1 To get a specific item (need auction id)
                            2 To Bid
                            3 to see whole list
                            4 to logout
                            """);
            answer = Validation.validateInteger(scanner.nextLine());
            System.out.println();
            if(answer == 1)
                getAuctionItem(server, user);
            else if(answer == 2)
                bidItem(server, user);
            else if(answer == 3)
                printList(server, user);
        } while(answer != 4);
    }
    private static void getAuctionItem(IBuyer server, User user) {
        try {
            System.out.print("Please enter an id: ");
            var sealedObject = server.getSpec(
                    Validation.validateInteger(new Scanner(System.in).nextLine()), createSealedRequest(user));

            var itemKey = encryptionService.decryptSecretKey(Constants.PASSWORD,
                    Constants.ITEM_SECRET_KEY_ALIAS, Constants.ITEM_SECRET_KEY_PATH);

            var item = (AuctionItem) sealedObject.getObject(itemKey);

            if(item.isEmpty())
                System.out.println("There's no item with the requested id");
            else
                System.out.println(item);
        } catch (IOException | ClassNotFoundException | NoSuchAlgorithmException | InvalidKeyException exception) {
            System.out.println("ERROR:\t problem while trying to get the object");
            exception.printStackTrace();
        }
    }
    private static void bidItem(IBuyer server, User user) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Please enter the auction id: ");
        int auctionId = Validation.validateInteger(scanner.nextLine());
        System.out.print("Please enter the bidding amount: ");
        int bid = Validation.validateInteger(scanner.nextLine());
        System.out.println();
        try {
            var response = server.bidItem(new BidRequest(auctionId, bid),
                    createSealedRequest(user));
            if(response.hasItem() && response.bidComparison() > 0)
                System.out.println("Bid successfully");
            else if(!response.hasItem())
                System.out.println("There is no auction with that id");
            else if(response.startingPriceComparison() == 0)
                System.out.println("The bid is equal to the starting price");
            else if(response.startingPriceComparison() == -1)
                System.out.println("The bid is lower than the starting price");
            else if(response.bidComparison() == 0)
                System.out.println("The bid is equal to the current bid");
            else if(response.bidComparison() == -1)
                System.out.println("The bid is lower than the current bid");

        } catch (RemoteException exception) {
            System.out.println("ERROR:\t couldn't bid the item");
            throw new RuntimeException(exception);
        }
    }
    private static void printList(IBuyer server, User user) {
        try {
            var sealedObject = server.getList(createSealedRequest(user));
            var listKey = encryptionService.decryptSecretKey(Constants.PASSWORD,
                    Constants.LIST_SECRET_KEY_ALIAS, Constants.LIST_SECRET_KEY_PATH);
            @SuppressWarnings("unchecked")
            var hashMap = (Map<Integer, AuctionItem>) sealedObject.getObject(listKey);
            if(hashMap == null) return;

            List<AuctionItem> items = new ArrayList<>(hashMap.size());

            hashMap.forEach((key, value) -> items.add(value));

            items.sort(Comparator.reverseOrder());

            if(items.size() == 0)
                System.out.println("There is no item in the auction list");
            else
                items.forEach(System.out::println);

        }
        catch (IOException | ClassNotFoundException | NoSuchAlgorithmException | InvalidKeyException exception) {
            System.out.println("ERROR:\t couldn't print the list");
            throw new RuntimeException(exception);
        }
    }
}
