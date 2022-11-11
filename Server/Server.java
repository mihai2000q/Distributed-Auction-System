import javax.crypto.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Server implements IBuyer, ISeller, IServer {
    private final Map<Integer, AuctionItem> auctionItems;
    private final List<User> users;
    private final List<User> activeUsers;
    private final IEncryptionService encryptionService;
    public Server() {
        super();
        encryptionService = new EncryptionService();
        auctionItems = new HashMap<>(Constants.LIST_CAPACITY);
        users = new ArrayList<>(Constants.USERS_CAPACITY);
        activeUsers = new ArrayList<>();
        loadList();
        initUsers();
    }
    public static void main(String[] args) {
        //launching the server
        try {
            Server server = new Server();
            IServer stub = (IServer) UnicastRemoteObject.exportObject(server, Constants.SERVER_PORT);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(Constants.SERVER_NAME, stub);
            System.out.println("Server ready\n");
        } catch (Exception e) {
            System.err.println("Error\t: problem while trying to launch the server");
            e.printStackTrace();
        }
    }
    @Override
    public boolean login(User user, SealedObject clientRequest) throws RemoteException {
        readClientRequest(clientRequest);
        if(!users.contains(user)) {
            System.out.println("Failed login attempt");
            return false;
        }
        activeUsers.add(user);
        System.out.println("Number of active users is " + activeUsers.size());
        return true;
    }
    @Override
    public void logout(User user, SealedObject clientRequest) throws RemoteException {
        readClientRequest(clientRequest);
        activeUsers.remove(user);
        System.out.println("Number of active users is " + activeUsers.size());
    }
    public AuctionItem getSpec(int auctionId){
        return auctionItems.getOrDefault(auctionId, AuctionItem.Empty);
    }
    @Override
    public SealedObject getSpec(int auctionId, SealedObject clientRequest) throws RemoteException {
        var item = getSpec(auctionId);
        readClientRequest(clientRequest);
        return encryptionService.encryptObject(item, Constants.ENCRYPTION_ALGORITHM,
                Constants.PASSWORD, Constants.ITEM_SECRET_KEY_ALIAS, Constants.ITEM_SECRET_KEY_PATH);
    }
    @Override
    public boolean bidItem(BidRequest request, SealedObject clientRequest) {
        readClientRequest(clientRequest);
        if(!auctionItems.containsKey(request.auctionId()))
            return false;
        var item = auctionItems.get(request.auctionId());
        item.setNewBid(request.bidder(), request.bid());
        if(item.getCurrentBid() >= item.getReservePrice()) {
            System.out.println(item.getItemName() + " has reached the reserve price of " + item.getReservePrice() +
            " with a bid of " + item.getCurrentBid());
            closeAuction(request.auctionId(), null);
        }
        else
            System.out.println(item.getHighestBidName() + " changed the bidding of the item " +
                    "with the auction id " + request.auctionId());
        return true;
    }
    @Override
    public Pair<Boolean, Integer> createAuction(SealedObject sealedItem, SealedObject clientRequest)
            throws RemoteException {
        var secretKey = encryptionService.decryptSecretKey(Constants.PASSWORD,
                Constants.ITEM_SECRET_KEY_ALIAS, Constants.ITEM_SECRET_KEY_PATH);
        AuctionItem item;
        int itemId = Constants.generateRandomInt();

        //in case there is an item with the same ID already, compute another one
        for(AuctionItem element : auctionItems.values())
            while(element.getId() == itemId)
                itemId = Constants.generateRandomInt();

        readClientRequest(clientRequest);
        try {
            item = (AuctionItem) sealedItem.getObject(secretKey);
            item = item.setNewId(itemId, item);
        } catch (IOException | ClassNotFoundException | NoSuchAlgorithmException | InvalidKeyException exception) {
            System.out.println("ERROR:\t couldn't unlock the sealed item");
            throw new RuntimeException(exception);
        }
        if(auctionItems.containsValue(item))
            return new Pair<>(false, -1);
        //compute an auctionId
        int auctionId = Constants.generateRandomInt();
        for(Integer element : auctionItems.keySet())
            while(element == auctionId)
                auctionId = Constants.generateRandomInt();
        auctionItems.put(auctionId,item);
        saveList();
        System.out.println("Received item with id: " + item.getId() + "\n");
        System.out.println("Created auction with the id: " + auctionId + "\n");
        return new Pair<>(true, auctionId);
    }
    @Override
    public Pair<Boolean, String> closeAuction(int auctionId, SealedObject clientRequest) {
        if(clientRequest != null)
            readClientRequest(clientRequest);
        if(!auctionItems.containsKey(auctionId))
            return new Pair<>(false, "N/A");
        var item = auctionItems.get(auctionId);
        System.out.println("Closed auction for the item with the id: " + auctionItems.remove(auctionId).getId());
        if(item.isBid())
            System.out.println(item.getHighestBidName() + " won with a bidding of " + item.getCurrentBid());
        else
            System.out.println("Nobody bid");
        System.out.println();
        return new Pair<>(true, item.getHighestBidName());
    }
    @Override
    public SealedObject getList(SealedObject clientRequest) throws RemoteException {
        readClientRequest(clientRequest);
        System.out.println("Sent the whole list\n");
        return encryptionService.encryptObject(auctionItems,
                Constants.ENCRYPTION_ALGORITHM, Constants.PASSWORD,
                Constants.LIST_SECRET_KEY_ALIAS, Constants.LIST_SECRET_KEY_PATH);
    }
    private void readClientRequest(SealedObject clientRequest) {
        var requestKey = encryptionService.decryptSecretKey(Constants.PASSWORD,
                Constants.REQUEST_SECRET_KEY_ALIAS, Constants.REQUEST_SECRET_KEY_PATH);
        ClientRequest request;
        try {
            request = (ClientRequest) clientRequest.getObject(requestKey);
        } catch (IOException | ClassNotFoundException | NoSuchAlgorithmException | InvalidKeyException e) {
            System.out.println("ERROR:\t couldn't unlock the client request");
            throw new RuntimeException(e);
        }

        System.out.println("\n--------------------------\n");
        System.out.println(request);
        System.out.println();
    }
    private void loadList() {
        if(!Files.exists(Path.of(Constants.LIST_PATH)))
            return;
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(Constants.LIST_PATH));
            var outputList = (HashMap<Integer, AuctionItem>) objectInputStream.readObject();
            if(outputList != null)
                auctionItems.putAll(outputList);
            objectInputStream.close();
        } catch (IOException exception) {
            System.out.println("ERROR:\t problem while trying to read the list file");
            throw new RuntimeException(exception);
        } catch (ClassNotFoundException e) {
            System.out.println("ERROR:\t couldn't recreate the list");
            throw new RuntimeException(e);
        }
    }
    private void saveList() {
        try {
            if(!Files.exists(Path.of(Constants.LIST_PATH)))
                Files.createDirectory(Path.of(Constants.LIST_PATH.split("/")[0]));
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(Constants.LIST_PATH));
            objectOutputStream.writeObject(auctionItems);
            objectOutputStream.close();
        } catch (IOException exception) {
            System.out.println("ERROR:\t problem while creating the list file");
            throw new RuntimeException(exception);
        }
    }
    private void initUsers() {
        users.add(new User("admin", "admin", Constants.ClientType.Seller));
        users.add(new User("admin", "admin", Constants.ClientType.Buyer));
    }
}
