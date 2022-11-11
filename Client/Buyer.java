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
        final IEncryptionService encryptionService = new EncryptionService();
        IBuyer server = null;
        final Integer[] answers = {1, 2, 3};
        int answer;
        Scanner scanner = new Scanner(System.in);
        server = connectToServer(server);
        do {
            System.out.println("""
                          
                            Please type
                            
                            1 To get a specific item (need auction id)
                            2 To Bid
                            3 to see whole list
                            """);
            answer = scanner.nextInt();
        } while (!contains(answers, answer));
        if(answer == 1)
            getAuctionItem(server, encryptionService);
        else if(answer == 2)
            bidItem(server, encryptionService);
        else
            printList(server, encryptionService);
    }
    private static void getAuctionItem(IBuyer server, IEncryptionService encryptionService) {
        try {
            System.out.println("Please enter an id:\t");
            var sealedObject = server.getSpec(new Scanner(System.in).nextInt(),
                    createSealedRequest(encryptionService));

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
    private static void bidItem(IBuyer server, IEncryptionService encryptionService) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please enter the auction id:\t");
        int auctionId = scanner.nextInt();
        System.out.println("Please enter the bidding amount:\t");
        int bid = scanner.nextInt();
        try {
            if(server.bidItem(new BidRequest(auctionId, bid, "Buyer"), createSealedRequest(encryptionService)))
                System.out.println("Bid successfully");
            else
                System.out.println("There is no auction with that id");
        } catch (RemoteException exception) {
            System.out.println("ERROR:\t couldn't bid the item");
            throw new RuntimeException(exception);
        }
    }
    private static void printList(IBuyer server, IEncryptionService encryptionService) {
        try {
            var sealedObject = server.getList(createSealedRequest(encryptionService));
            var listKey = encryptionService.decryptSecretKey(Constants.PASSWORD,
                    Constants.LIST_SECRET_KEY_ALIAS, Constants.LIST_SECRET_KEY_PATH);
            var hashMap = (HashMap<Integer, AuctionItem>) sealedObject.getObject(listKey);
            List<AuctionItem> items = new ArrayList<>(hashMap.size());

            hashMap.forEach((key, value) -> items.add(value));

            items.sort(Comparator.reverseOrder());
            if(items.size() == 0)
                System.out.println("There is no item in the auction list");
            else
                items.forEach(System.out::println);

            System.out.println();

            //hashMap.forEach((key, value) -> System.out.println("The auction id is:\t" + key + " " + value));
        }
        catch (IOException | ClassNotFoundException | NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new RuntimeException(exception);
        }
    }
}
