import java.rmi.RemoteException;
import java.util.Scanner;

public final class Seller extends Client {
    public Seller() {
        super();
    }
    public static void main(String[] args) {
        var response = connectToServer(Constants.ClientType.Seller);
        if(response == null)
            return;
        final ISeller server = (ISeller) response.x();
        final User user = response.y();
        int answer;
        Scanner scanner = new Scanner(System.in);
        do {
            System.out.println("""
                           
                           Please type
                            
                            1 To create an auction
                            2 To remove an auction
                            3 To logout
                            """);
            answer = scanner.nextInt();
            if(answer == 1)
                createAuction(server, user);
            else if(answer == 2)
                closeAuction(server, user);
        } while(answer != 3);
        System.exit(0);
    }
    private static void createAuction(ISeller server, User user) {
        String itemName;
        int startingPrice;
        int reservePrice;
        Scanner scanner = new Scanner(System.in);
        System.out.print("Please enter the item name: ");
        itemName = scanner.nextLine();
        System.out.print("What is the starting price: ");
        startingPrice = scanner.nextInt();
        System.out.print("What is the reserve price: ");
        reservePrice = scanner.nextInt();
        System.out.print("What is the description of the item: ");
        scanner.nextLine();
        String description = scanner.nextLine();
        //the id will be computed later in the server
        final AuctionItem item = new AuctionItem(-1, itemName, "Seller",
                                                startingPrice, reservePrice, description);

        var sealedItem = encryptionService.encryptObject(item, Constants.ENCRYPTION_ALGORITHM,
                Constants.PASSWORD, Constants.ITEM_SECRET_KEY_ALIAS, Constants.ITEM_SECRET_KEY_PATH);

        try {
            var response = server.createAuction(sealedItem, createSealedRequest(user));
            if (response.x())
                System.out.println("\nItem added successfully with the auction ID " + response.y());
            else
                System.out.println("\nItem already in the list");
        } catch (RemoteException exception) {
            System.out.println("ERROR:\t couldn't add the item");
            throw new RuntimeException(exception);
        }
    }
    private static void closeAuction(ISeller server, User user) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please enter the auction id:\t");
        int auctionId = scanner.nextInt();
        try {
            var response = server.closeAuction(auctionId, createSealedRequest(user));
            if(response.hasItem() && response.isAuthorized())
                System.out.println("Auction " + auctionId + " closed with success " +
                        (response.winner().equals("None") ?
                                "but nobody bid" :
                                "and " + response.winner() + " won"));
            else if(!response.isAuthorized())
                System.out.println("You are not authorized to close this auction!");
            else
                System.out.println("There is no auction with that id");

        } catch (RemoteException exception) {
            System.out.println("ERROR:\t couldn't close auction");
            throw new RuntimeException(exception);
        }
    }
}
