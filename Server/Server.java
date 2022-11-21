import javax.crypto.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

public final class Server implements IBuyer, ISeller {
    private int serverChallengeNumber;
    private int clientChallengeNumber;
    private final Map<Integer, AuctionItem> auctionItems;
    private final Map<Integer, List<Bid>> bids;
    private final List<User> users;
    private final Collection<User> activeUsers;
    private final IEncryptionService encryptionService;
    public Server() {
        super();
        encryptionService = new EncryptionService();
        auctionItems = new HashMap<>(Constants.LIST_CAPACITY);
        bids = new HashMap<>();
        users = new ArrayList<>(Constants.USERS_CAPACITY);
        activeUsers = new ArrayList<>();
        loadMapFile(Constants.AUCTION_LIST_PATH, auctionItems);
        loadMapFile(Constants.BIDS_PATH, bids);
        initUsers();
    }
    public static void main(String[] args) {
        //launching the server
        try {
            Server server = new Server();
            Remote stub = UnicastRemoteObject.exportObject(server, Constants.SERVER_PORT);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(Constants.SERVER_NAME, stub);
            System.out.println("Server ready\n");
        } catch (RemoteException exception) {
            System.err.println("Error\t: problem while trying to launch the server");
            throw new RuntimeException(exception);
        }
    }
    @Override
    public int requestServerChallenge(SealedObject clientRequest) throws RemoteException {
        readClientRequest(clientRequest);
        serverChallengeNumber = Constants.generateRandomInt();
        System.out.println("Server sent authentication challenge for client");
        return serverChallengeNumber;
    }
    @Override
    public boolean sendEncryptedServerChallenge(SealedObject challenge, SealedObject clientRequest)
            throws RemoteException {
        try {
            readClientRequest(clientRequest);
            int number = (int) challenge.getObject(
                    (encryptionService.decryptSecretKey(Constants.PASSWORD,
                    Constants.AUTHENTICATION_KEY_ALIAS, Constants.AUTHENTICATION_SECRET_KEY_PATH)));
            if(number == serverChallengeNumber) {
                System.out.println("Server authenticated the client");
                return true;
            }
            else {
                System.out.println("Server couldn't authenticate the client");
                return false;
            }
        } catch (IOException | ClassNotFoundException | NoSuchAlgorithmException | InvalidKeyException exception) {
            System.out.println("ERROR:\t problem while decrypting the random number");
            throw new RuntimeException(exception);
        }
    }
    @Override
    public boolean sendClientChallenge(int clientChallenge, SealedObject clientRequest) throws RemoteException {
        readClientRequest(clientRequest);
        clientChallengeNumber = clientChallenge;
        System.out.println("Server received the client challenge");
        return true;
    }
    @Override
    public SealedObject requestEncryptedClientChallenge(SealedObject clientRequest) throws RemoteException {
        readClientRequest(clientRequest);
        System.out.println("Server got authenticated by client");
        return encryptionService.encryptObject(clientChallengeNumber,
                Constants.ENCRYPTION_ALGORITHM, Constants.PASSWORD,
                Constants.AUTHENTICATION_KEY_ALIAS, Constants.AUTHENTICATION_SECRET_KEY_PATH);
    }
    @Override
    public boolean login(SealedObject clientRequest) throws RemoteException {
        var user = readClientRequest(clientRequest).getUser();
        if(!users.contains(user)) {
            System.out.println("Failed login attempt");
            return false;
        }
        activeUsers.add(user);
        System.out.println("Number of active users is " + activeUsers.size());
        return true;
    }
    @Override
    public void logout(SealedObject clientRequest) throws RemoteException {
        activeUsers.remove(readClientRequest(clientRequest).getUser());
        System.out.println("Number of active users is " + activeUsers.size());
    }
    public AuctionItem getSpec(int auctionId){
        return auctionItems.getOrDefault(auctionId, AuctionItem.EMPTY);
    }
    @Override
    public SealedObject getSpec(int auctionId, SealedObject clientRequest) throws RemoteException {
        readClientRequest(clientRequest);
        var item = getSpec(auctionId);
        item = !item.isOngoing() ? AuctionItem.EMPTY : item;
        System.out.println("Sent details on auction: " + auctionId);
        return encryptionService.encryptObject(item, Constants.ENCRYPTION_ALGORITHM,
                Constants.PASSWORD, Constants.ITEM_SECRET_KEY_ALIAS, Constants.ITEM_SECRET_KEY_PATH);
    }
    @Override
    public BidResponse bidItem(SealedObject BidRequest, SealedObject clientRequest) {
        var request = readClientRequest(clientRequest);
        var user = request.getUser();
        var bidComparison = -1;
        BidRequest bidRequest;
        try {
            bidRequest = (BidRequest) BidRequest.getObject(encryptionService.decryptSecretKey(
                    Constants.PASSWORD, Constants.REQUEST_SECRET_KEY_ALIAS, Constants.REQUEST_SECRET_KEY_PATH));
        } catch (IOException | ClassNotFoundException |
                 NoSuchAlgorithmException | InvalidKeyException exception) {
            System.out.println("ERROR:\t couldn't decrypt the key while bidding");
            throw new RuntimeException(exception);
        }
        var auctionId = bidRequest.auctionId();
        if(!auctionItems.containsKey(auctionId))
            return new BidResponse(false, bidComparison);
        var item = auctionItems.get(auctionId);
        var isLowerThanReservedPrice = bidRequest.bid() < item.getReservePrice();

        List<Bid> query = new ArrayList<>();
        if(bids.get(auctionId).size() == 0)
            System.out.println("First bid on \"" + item.getItemName() + "\" !!!!");
        else
            query = bids.get(auctionId).stream().filter(x ->
                    Objects.equals(x.getUsername(), user.getUsername())).collect(Collectors.toList());

        //each and every user should have ONLY one bid on an item at a time
        if (query.size() > 1)
            throw new RuntimeException("ERROR:\t too many elements in the bid hash map");

        var result = query.size() == 0 ? Bid.EMPTY : query.get(0);

        if(isLowerThanReservedPrice)
            System.out.println("The user " + user.getId() +
                    " hasn't reached the reserved price of " + item.getReservePrice());
        else if (result.isEmpty()) {
            System.out.println(user.getUsername() +
                    " made its first bid on \"" + item.getItemName() + "\"");
            bids.get(auctionId).add(new Bid(Constants.generateRandomInt(), item.getItemName(),
                                            user.getUsername(), bidRequest.bid()));
            saveFile(Constants.BIDS_PATH, bids);
        }
        else {
            System.out.println(user.getUsername() +
                    " changed its bid for \"" + item.getItemName() + "\" from " +
                    result.getBid() + " to " + bidRequest.bid());
            bids.get(auctionId).remove(result);
            bids.get(auctionId).add(new Bid(result.getId(), result.getItemName(),
                    result.getUsername(), bidRequest.bid()));
            saveFile(Constants.BIDS_PATH, bids);
        }

        if(bidRequest.bid() >= item.getCurrentBid()) {
            item.setNewBid(user.getUsername(), bidRequest.bid());
            bidComparison = bidRequest.bid() == item.getCurrentBid() ? 0 : 1;
            bidComparison = isLowerThanReservedPrice ? 0 : bidComparison; //just lie the buyer
        }
        return new BidResponse(true, bidComparison);
    }
    @Override
    public SealedObject getList(SealedObject clientRequest) throws RemoteException {
        readClientRequest(clientRequest);
        System.out.println("Sent the whole list");
        return encryptionService.encryptObject(
                getAllOngoingItems().stream().sorted(Comparator.reverseOrder()).toList(),
                Constants.ENCRYPTION_ALGORITHM, Constants.PASSWORD,
                Constants.LIST_SECRET_KEY_ALIAS, Constants.LIST_SECRET_KEY_PATH);
    }
    @Override
    public InfoResponse getInfoOnAuction(int auctionId, SealedObject clientRequest) throws RemoteException {
        var user = readClientRequest(clientRequest).getUser();
        if(!auctionItems.containsKey(auctionId))
            return new InfoResponse(false, false, true, false);
        System.out.println("Sent info about the auction: " + auctionId);
        if (!getParticipantsForItem(auctionId).contains(user.getUsername()))
            return new InfoResponse(true, false, true, false);
        else if(auctionItems.get(auctionId).isOngoing())
            return new InfoResponse(true, true, true, false);
        else if(!getWinnerForItem(auctionId).equals(user.getUsername()))
            return new InfoResponse(true, true, false, false);
        else
            return new InfoResponse(true, true, false, true);
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

        var request = readClientRequest(clientRequest);
        var user = request.getUser();
        try {
            item = (AuctionItem) sealedItem.getObject(secretKey);
            item = item.setNewId(itemId);
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
        item = item.setAuctionId(auctionId);
        auctionItems.put(auctionId,item);
        saveFile(Constants.AUCTION_LIST_PATH, auctionItems);
        System.out.println(Constants.SERVER_NAME + " received item with id: " + item.getId() + "\n");
        System.out.println(user.getUsername() + " created auction with the id: " + auctionId + "\n");
        bids.put(auctionId, new ArrayList<>());
        saveFile(Constants.BIDS_PATH, bids);
        return new Pair<>(true, auctionId);
    }
    @Override
    public CloseAuctionResponse closeAuction(int auctionId, SealedObject clientRequest) throws RemoteException {
        var request = readClientRequest(clientRequest);
        var user = request.getUser();
        if(!auctionItems.containsKey(auctionId))
            return new CloseAuctionResponse(false, false, true,"N/A");
        var item = auctionItems.get(auctionId);
        if(!Objects.equals(user.getUsername(), item.getSellerName()))
            return new CloseAuctionResponse(true, false, true,"N/A");
        if(!item.isOngoing())
            return new CloseAuctionResponse(true, true, true,"N/A");
        closeAuctionByItemId(auctionId);
        return new CloseAuctionResponse(true, true, false, item.getHighestBidName());
    }
    @Override
    public SealedObject getBids(int auctionId, SealedObject clientRequest) throws RemoteException {
        readClientRequest(clientRequest);
        var list = bids.getOrDefault(auctionId, new ArrayList<>())
                                .stream().sorted(Comparator.reverseOrder()).toList();
        System.out.println("Sent bids for auction: " + auctionId);
        return encryptionService.encryptObject(list,
                Constants.ENCRYPTION_ALGORITHM, Constants.PASSWORD,
                Constants.LIST_SECRET_KEY_ALIAS, Constants.LIST_SECRET_KEY_PATH);
    }
    private void closeAuctionByItemId(int auctionId) {
        var item = auctionItems.remove(auctionId);
        item.setOngoing(false);
        auctionItems.put(auctionId, item);
        saveFile(Constants.AUCTION_LIST_PATH, auctionItems);
        System.out.println(Constants.SERVER_NAME +
                " closed the auction with id: " + auctionId);
        User user = users.stream().filter(x -> Objects.equals(x.getUsername(), item.getHighestBidName()))
                    .findFirst().orElse(User.EMPTY);
        if(item.isBid()) {
            System.out.println("The user with the id " + user.getId()
                    + " won the \"" + item.getItemName() + "\" with a bid of " + item.getCurrentBid());
        }
        else
            System.out.println("Nobody bid for the \"" + item.getItemName() + "\"");
    }
    private ClientRequest readClientRequest(SealedObject clientRequest) {
        var requestKey = encryptionService.decryptSecretKey(Constants.PASSWORD,
                Constants.CLIENT_REQUEST_SECRET_KEY_ALIAS, Constants.CLIENT_REQUEST_SECRET_KEY_PATH);
        ClientRequest request;
        try {
            request = (ClientRequest) clientRequest.getObject(requestKey);
        } catch (IOException | ClassNotFoundException |
                 NoSuchAlgorithmException | InvalidKeyException exception) {
            System.out.println("ERROR:\t couldn't unlock the client request");
            throw new RuntimeException(exception);
        }

        System.out.println("\n--------------------------\n");
        System.out.println(request);
        System.out.println();
        return request;
    }
    private <T, E> void loadMapFile(String path, Map<T, E> map) {
        if(!Files.exists(Path.of(path)))
            return;
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(path));
            @SuppressWarnings("unchecked")
            var outputList = (Map<T, E>) objectInputStream.readObject();
            if(outputList != null)
                map.putAll(outputList);
            objectInputStream.close();
        } catch (IOException exception) {
            System.out.println("ERROR:\t problem while trying to read the file");
            throw new RuntimeException(exception);
        } catch (ClassNotFoundException exception) {
            System.out.println("ERROR:\t couldn't recreate the list");
            throw new RuntimeException(exception);
        }
    }
    private void saveFile(String path, Object map) {
        try {
            var directoryPath = Path.of(path.split("/")[0]);
            if(!Files.exists(directoryPath))
                Files.createDirectory(directoryPath);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(path));
            objectOutputStream.writeObject(map);
            objectOutputStream.close();
        } catch (IOException exception) {
            System.out.println("ERROR:\t problem while creating the file");
            throw new RuntimeException(exception);
        }
    }
    private void initUsers() {
        users.add(new User(Constants.generateRandomInt(),
                "james", "james123", Constants.ClientType.Seller));
        users.add(new User(Constants.generateRandomInt(),
                "chris", "chris123", Constants.ClientType.Seller));
        users.add(new User(Constants.generateRandomInt(),
                "edward", "edi123", Constants.ClientType.Buyer));
        users.add(new User(Constants.generateRandomInt(),
                "michael", "michael123", Constants.ClientType.Buyer));
    }
    private List<AuctionItem> getAllOngoingItems() {
        return auctionItems.values().stream().filter(AuctionItem::isOngoing).toList();
    }
    private List<String> getParticipantsForItem(int auctionId) {
        var participants = new ArrayList<String>();
        bids.get(auctionId).forEach(x -> participants.add(x.getUsername()));
        return participants;
    }
    private String getWinnerForItem(int auctionId){
        return auctionItems.get(auctionId).getHighestBidName();
    }
}
