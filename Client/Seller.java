import java.rmi.RemoteException;
import java.util.Scanner;

public class Seller extends Client {
    public Seller() {
        super();
    }
    public static void main(String[] args) {
        final IEncryptionService encryptionService = new EncryptionService();
        ISeller server = null;
        final Integer[] answers = {1, 2, 3};
        int answer;
        Scanner scanner = new Scanner(System.in);
        server = connectToServer(server);
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
    private static void createAuction(ISeller server, IEncryptionService encryptionService) {
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
    private static void closeAuction(ISeller server, IEncryptionService encryptionService) {
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
}
