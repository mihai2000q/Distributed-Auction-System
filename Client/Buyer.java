import java.io.IOException;
import java.rmi.RemoteException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Buyer extends Client {
    public Buyer() {
        super();
    }
    public static void main(String[] args) {
        IBuyer server = connectToServer(Constants.ClientType.Buyer);
        if(server == null)
            return;
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
            answer = scanner.nextInt();
            if(answer == 1)
                getAuctionItem(server);
            else if(answer == 2)
                bidItem(server);
            else if(answer == 3)
                printList(server);
        } while(answer != 4);
    }
    private static void getAuctionItem(IBuyer server) {
        try {
            System.out.print("Please enter an id:\t");
            var sealedObject = server.getSpec(new Scanner(System.in).nextInt(),
                    createSealedRequest());

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
    private static void bidItem(IBuyer server) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please enter the auction id:\t");
        int auctionId = scanner.nextInt();
        System.out.println("Please enter the bidding amount:\t");
        int bid = scanner.nextInt();
        System.out.println();
        try {
            if(server.bidItem(new BidRequest(auctionId, bid, "Buyer"), createSealedRequest()))
                System.out.println("Bid successfully");
            else
                System.out.println("There is no auction with that id");
        } catch (RemoteException exception) {
            System.out.println("ERROR:\t couldn't bid the item");
            throw new RuntimeException(exception);
        }
    }
    private static void printList(IBuyer server) {
        try {
            var sealedObject = server.getList(createSealedRequest());
            var listKey = encryptionService.decryptSecretKey(Constants.PASSWORD,
                    Constants.LIST_SECRET_KEY_ALIAS, Constants.LIST_SECRET_KEY_PATH);
            var hashMap = (HashMap<Integer, AuctionItem>) sealedObject.getObject(listKey);
//            List<AuctionItem> items = new ArrayList<>(hashMap.size());
//
//            hashMap.forEach((key, value) -> items.add(value));
//
//            items.sort(Comparator.reverseOrder());
//            if(items.size() == 0)
//                System.out.println("There is no item in the auction list");
//            else
//                items.forEach(System.out::println);
//
//            System.out.println();

            hashMap.forEach((key, value) -> System.out.println("The auction id is: " + key + " " + value));
        }
        catch (IOException | ClassNotFoundException | NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new RuntimeException(exception);
        }
    }
}
