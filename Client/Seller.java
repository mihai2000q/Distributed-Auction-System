import java.io.IOException;
import java.rmi.RemoteException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

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
        do {
            System.out.println("""
                           ------------------------------------------
                           
                           Please type
                            
                            1 To create an auction
                            2 To remove an auction
                            3 To get current bids on item
                            4 To logout
                            """);
            answer = Validation.validateInteger(scanner.nextLine());
            System.out.println();
            switch (answer) {
                case 1 -> createAuction(server, user);
                case 2 -> closeAuction(server, user);
                case 3 -> getBids(server, user);
            }
        } while(answer != 4);
        scanner.close();
        System.exit(0);
    }
    private static void createAuction(ISeller server, User user) {
        String itemName;
        int startingPrice;
        int reservePrice;
        System.out.print("Please enter the item name: ");
        itemName = Normalization.normalizeString(scanner.nextLine());
        System.out.print("What is the reserve price: ");
        reservePrice = Validation.validateInteger(scanner.nextLine());
        if(reservePrice == -1) return;
        System.out.print("What is the starting price: ");
        startingPrice = Validation.validateInteger(scanner.nextLine());
        if(startingPrice == -1) return;
        if(reservePrice > startingPrice) {
            System.out.println("\nThe reserved price cannot be higher than the starting price");
            return;
        }
        System.out.print("What is the description of the item: ");
        String description = Normalization.normalizeString(scanner.nextLine());
        AuctionItemDto auctionItemDto = new AuctionItemDto(itemName, user.getUsername(),
                startingPrice, reservePrice, description);

        var sealedItem = encryptionService.encryptObject(auctionItemDto, Constants.ENCRYPTION_ALGORITHM,
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
        System.out.print("Please enter the auction id: ");
        int auctionId = Validation.validateInteger(scanner.nextLine());
        if(auctionId == -1) return;
        System.out.println();
        try {
            var response = server.closeAuction(auctionId, createSealedRequest(user));
            if(!response.hasItem())
                System.out.println("There is no auction with that id");
            else if(!response.isAuthorized())
                System.out.println("You are not authorized to close this auction!");
            else if(response.isClosedAlready())
                System.out.println("You have closed this auction already");
            else
            System.out.println("Auction " + auctionId + " closed with success " +
                    (response.winner().equals("None") ?
                            "but nobody bid" :
                            "and " + response.winner() + " won"));
        } catch (RemoteException exception) {
            System.out.println("ERROR:\t couldn't close auction");
            throw new RuntimeException(exception);
        }
    }
    @SuppressWarnings("unchecked")
    private static void getBids(ISeller server, User user) {
        System.out.print("Please enter the auction id: ");
        int auctionId = Validation.validateInteger(scanner.nextLine());
        if(auctionId == -1) return;
        System.out.println();
        try {
            var response = server.getBids(auctionId, createSealedRequest(user));
            if(response.y() == -1)
                System.out.println("There's no such auction");
            else if (response.y() == 0)
                System.out.println("You are not authorized to access this information");
            else {
                var bids = (List<Bid>) response.x().getObject(encryptionService.decryptSecretKey(
                        Constants.PASSWORD, Constants.LIST_SECRET_KEY_ALIAS, Constants.LIST_SECRET_KEY_PATH));
                System.out.println();
                if (bids.size() == 0)
                    System.out.println("There are no bids for this auction");
                else
                    bids.forEach(System.out::println);
            }

        } catch (RemoteException exception) {
            System.out.println("ERROR:\t couldn't get the bids");
            throw new RuntimeException(exception);
        } catch (IOException | NoSuchAlgorithmException |
                 InvalidKeyException | ClassNotFoundException exception) {
            System.out.println("ERROR:\t couldn't decrypt the list with bids");
            throw new RuntimeException(exception);
        }
    }
}
