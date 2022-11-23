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
        do {
            System.out.println("""
                            ------------------------------------------
                            
                            Please type
                            
                            1 To get a specific item (need auction id)
                            2 To Bid
                            3 to see whole list
                            4 to get info an a item
                            5 to logout
                            """);
            answer = Validation.validateInteger(scanner.nextLine());
            System.out.println();
            switch (answer) {
                case 1 -> getAuctionItem(server, user);
                case 2 -> bidItem(server, user);
                case 3 -> printList(server, user);
                case 4 -> getInfoOnAuction(server, user);
            }
        } while(answer != 5);
        scanner.close();
        System.exit(0);
    }
    private static void getAuctionItem(IBuyer server, User user) {
        try {
            System.out.print("Please enter an id: ");
            int auctionId = Validation.validateInteger(new Scanner(System.in).nextLine());
            if(auctionId == -1) return;
            var sealedObject = server.getSpec(auctionId, createSealedRequest(user));

            var itemKey = encryptionService.decryptSecretKey(Constants.PASSWORD,
                    Constants.ITEM_SECRET_KEY_ALIAS, Constants.ITEM_SECRET_KEY_PATH);

            var item = (AuctionItem) sealedObject.getObject(itemKey);

            System.out.println();
            if(item.isEmpty())
                System.out.println("There's no item with the requested id");
            else
                System.out.println(item);
        } catch (IOException | ClassNotFoundException |
                 NoSuchAlgorithmException | InvalidKeyException exception) {
            System.out.println("ERROR:\t problem while trying to get the object");
            exception.printStackTrace();
        }
    }
    private static void bidItem(IBuyer server, User user) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Please enter the auction id: ");
        int auctionId = Validation.validateInteger(scanner.nextLine());
        if(auctionId == -1) return;
        System.out.print("Please enter the bidding amount: ");
        int bid = Validation.validateInteger(scanner.nextLine());
        if(bid == -1) return;
        System.out.println();
        try {
            var response = server.bidItem(
                    encryptionService.encryptObject(new BidRequest(auctionId, bid), Constants.ENCRYPTION_ALGORITHM,
                            Constants.PASSWORD, Constants.REQUEST_SECRET_KEY_ALIAS, Constants.REQUEST_SECRET_KEY_PATH),
                    createSealedRequest(user));
            if(!response.hasItem())
                System.out.println("There is no auction with that id");
            else if(!response.ongoing())
                System.out.println("The auction has closed");
            else if(response.isLowerThanReservedPrice())
                System.out.println("Couldn't bid as the reserved price hasn't been reached");
            else if(response.bidComparison() < 0)
                System.out.println("You cannot make a bid that is lower than the current one");
            else if(response.bidComparison() == 0)
                System.out.println("You cannot lower your bid");
            else
                System.out.println("Bid successful");

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
            var items = (List<AuctionItem>) sealedObject.getObject(listKey);
            if(items == null) return;
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
    private static void getInfoOnAuction(IBuyer server, User user) {
        try {
            System.out.print("Please enter the auction id: ");
            int auctionId = Validation.validateInteger(new Scanner(System.in).nextLine());
            if(auctionId == -1) return;
            var response = server.getInfoOnAuction(auctionId, createSealedRequest(user));
            System.out.println();
            if(!response.hasItem())
                System.out.println("There's no such auction with that id");
            else if(!response.isAuthorized())
                System.out.println("You are not authorized to access this information " +
                        "as you have not participated to this auction");
            else if(response.ongoing())
                System.out.println("The auction is still ongoing");
            else if (!response.isWinner())
                System.out.println("You are not the winner");
            else
                System.out.println("You are the winner");


        } catch (RemoteException exception) {
            System.out.println("ERROR:\t problem while trying to get info on an auction");
            throw new RuntimeException(exception);
        }
    }
}
