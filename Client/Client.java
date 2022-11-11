import javax.crypto.SealedObject;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Client {
    public static void main(String[] args) {
        final IEncryptionService encryptionService = new EncryptionService();
        final IServer server;
        final Integer[] answers = {1, 2, 3};
        int answer = -1;
        Scanner scanner = new Scanner(System.in);
        try {
            do {
                System.out.println("""
                        
                        Please type
                        
                        1 Are you a seller
                        2 Are you a buyer
                        """);
                answer = scanner.nextInt();
            } while (!contains(answers, answer));
        }
        catch (InputMismatchException exception) {
            System.out.println("\nPlease use only numbers!");
        }
        server = connectToServer();
        switch (answer) {
            case 1 -> {
                do {
                    System.out.println("""
                            
                            Please type
                            
                            1 To create an auction
                            2 To remove an auction
                            """);
                    answer = scanner.nextInt();
                } while (!contains(answers, answer));
                    if(answer == 1)
                        createAuction(server, encryptionService);
                    else
                        closeAuction(server, encryptionService);
            }
            case 2 -> {
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
        }
    }
    private static void getAuctionItem(IServer server, IEncryptionService encryptionService) {
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
    private static void bidItem(IServer server, IEncryptionService encryptionService) {
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
    private static void createAuction(IServer server, IEncryptionService encryptionService) {
        String itemName;
        int startingPrice;
        int reservePrice;
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please enter the item name: ");
        itemName = scanner.nextLine();
        System.out.println("What is the starting price: ");
        startingPrice = scanner.nextInt();
        System.out.println("What is the reserve price: ");
        reservePrice = scanner.nextInt();
        System.out.println("What is the description of the item: ");
        scanner.nextLine();
        String description = scanner.nextLine();
        //the id will be computed later in the server
        final AuctionItem item = new AuctionItem(-1, itemName, "Seller", startingPrice, reservePrice, description);

        var sealedItem = encryptionService.encryptObject(item, Constants.ENCRYPTION_ALGORITHM,
                Constants.PASSWORD, Constants.ITEM_SECRET_KEY_ALIAS, Constants.ITEM_SECRET_KEY_PATH);

        try {
            var response = server.createAuction(sealedItem, createSealedRequest(encryptionService));
            if (response.x())
                System.out.println("\nItem added successfully with the auction ID " + response.y());
            else
                System.out.println("\nItem already in the list");
        } catch (RemoteException exception) {
            System.out.println("ERROR:\t couldn't add the item");
            throw new RuntimeException(exception);
        }
    }
    private static void closeAuction(IServer server, IEncryptionService encryptionService) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please enter the auction id:\t");
        int auctionId = scanner.nextInt();
        try {
            var response = server.closeAuction(auctionId, createSealedRequest(encryptionService));
            if(response.x())
                System.out.println("Auction " + auctionId + " closed with success " +
                        (response.y().equals("None") ? "but nobody bid" : "and " + response.y() + " won"));
            else
                System.out.println("There is no auction with that id");
        } catch (RemoteException exception) {
            System.out.println("ERROR:\t couldn't close auction");
            throw new RuntimeException(exception);
        }
    }
    private static void printList(IServer server, IEncryptionService encryptionService) {
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
    private static SealedObject createSealedRequest(IEncryptionService encryptionService) {
        return encryptionService.encryptObject(new ClientRequest(Constants.generateRandomInt()),
                Constants.ENCRYPTION_ALGORITHM, Constants.PASSWORD,
                Constants.REQUEST_SECRET_KEY_ALIAS, Constants.REQUEST_SECRET_KEY_PATH);
    }
    private static <T> boolean contains(final T[] Array, final T variable) {
        for(final T element : Array)
            if(variable == element && variable != null)
                return true;
        return false;
    }
    private static IServer connectToServer() {
        IServer result;
        try {
            Registry registry = LocateRegistry.getRegistry(Constants.SERVER_HOST);
            result = (IServer) registry.lookup(Constants.SERVER_NAME);
        }
        catch (RemoteException | NotBoundException exception) {
            System.out.println("ERROR:\t problem while trying to connect to server");
            exception.printStackTrace();
            throw new RuntimeException(exception);
        }
        return result;
    }

}
