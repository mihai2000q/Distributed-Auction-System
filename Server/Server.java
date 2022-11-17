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
    private final Map<Integer, String> winners;
    private final Map<Integer, String> participants;
    private final List<User> users;
    private final Collection<User> activeUsers;
    private final IEncryptionService encryptionService;
    public Server() {
        super();
        encryptionService = new EncryptionService();
        auctionItems = new HashMap<>(Constants.LIST_CAPACITY);
        bids = new HashMap<>();
        winners = new HashMap<>();
        participants = new HashMap<>();
        users = new ArrayList<>(Constants.USERS_CAPACITY);
        activeUsers = new ArrayList<>();
        loadMapFile(Constants.LIST_PATH, auctionItems);
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
        } catch (Exception e) {
            System.err.println("Error\t: problem while trying to launch the server");
            e.printStackTrace();
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
    public boolean sendEncryptedServerChallenge(SealedObject randomNumber, SealedObject clientRequest)
            throws RemoteException {
        try {
            readClientRequest(clientRequest);
            int number = (int) randomNumber.getObject(
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
    public void sendClientChallenge(int randomNumber, SealedObject clientRequest) throws RemoteException {
        readClientRequest(clientRequest);
        clientChallengeNumber = randomNumber;
        System.out.println("Server received the client challenge");
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
        var item = getSpec(auctionId);
        readClientRequest(clientRequest);
        return encryptionService.encryptObject(item, Constants.ENCRYPTION_ALGORITHM,
                Constants.PASSWORD, Constants.ITEM_SECRET_KEY_ALIAS, Constants.ITEM_SECRET_KEY_PATH);
    }
    @Override
    public BidResponse bidItem(BidRequest bidRequest, SealedObject clientRequest) {
        var request = readClientRequest(clientRequest);
        var user = request.getUser();
        var auctionId = bidRequest.auctionId();
        if(!auctionItems.containsKey(auctionId))
            return new BidResponse(false, -1,-1);
        var item = auctionItems.get(auctionId);

        if(bidRequest.bid() < item.getStartingPrice())
            return new BidResponse(true, -1,-1);
        else if(bidRequest.bid() == item.getStartingPrice())
            return new BidResponse(true, 0,-1);

        if(bidRequest.bid() < item.getCurrentBid())
            return new BidResponse(true, 1,-1);
        else if(bidRequest.bid() == item.getCurrentBid())
            return new BidResponse(true, 1,0);

        List<Bid> query = new ArrayList<>();
        if(bids.get(auctionId).size() == 0)
            System.out.println("First bid on \"" + item.getItemName() + "\" !!!!");
        else
            query = bids.get(auctionId).stream().filter(x ->
                    Objects.equals(x.getUsername(), user.getUsername())).collect(Collectors.toList());

        //every user should have ONLY one bid on an item at a time
        if (query.size() > 1)
            throw new RuntimeException("ERROR:\t too many elements in the bid hash map");

        var result = query.size() == 0 ? Bid.EMPTY : query.get(0);

        if (result.isEmpty()) {
            System.out.println(user.getUsername() +
                    " made its first bid on " + item.getItemName());
            bids.get(auctionId).add(new Bid(Constants.generateRandomInt(), user.getUsername(), bidRequest.bid()));
            saveFile(Constants.BIDS_PATH, bids);
            participants.put(auctionId,user.getUsername());
        }
        else
            System.out.println(user.getUsername() +
                    " changed its bid for \"" + item.getItemName() + "\" from " +
                    result.getBid() + " to " + bidRequest.bid());
        bids.get(auctionId).remove(result);
        bids.get(auctionId).add(new Bid(result.getId(), result.getUsername(), bidRequest.bid()));
        saveFile(Constants.BIDS_PATH, bids);

        item.setNewBid(user.getUsername(), bidRequest.bid());
        if(item.getCurrentBid() >= item.getReservePrice()) {
            System.out.println("\"" + item.getItemName() + "\" has reached the reserve price of " +
                    item.getReservePrice() + " with a bid of " + item.getCurrentBid());
            closeAuctionByItemId(auctionId);
        }
        return new BidResponse(true, 1,1);
    }
    @Override
    public SealedObject getList(SealedObject clientRequest) throws RemoteException {
        readClientRequest(clientRequest);
        System.out.println("Sent the whole list");
        return encryptionService.encryptObject(auctionItems,
                Constants.ENCRYPTION_ALGORITHM, Constants.PASSWORD,
                Constants.LIST_SECRET_KEY_ALIAS, Constants.LIST_SECRET_KEY_PATH);
    }
    @Override
    public String getInfoOnAuction(int auctionId, SealedObject clientRequest) throws RemoteException {
        var user = readClientRequest(clientRequest).getUser();
        if(user.getUsername().equals(winners.get(auctionId)))
            return "You are the winner";
        else if (Objects.equals(participants.getOrDefault(auctionId, ""), ""))
            return "You are not the winner";
        else
            return "You are not authorized to access this information as you have not participated to this auction";
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
        saveFile(Constants.LIST_PATH, auctionItems);
        System.out.println(Constants.SERVER_NAME + " received item with id: " + item.getId() + "\n");
        System.out.println(user.getUsername() + " created auction with the id: " + auctionId + "\n");
        bids.put(auctionId, new ArrayList<>());
        saveFile(Constants.BIDS_PATH, bids);
        return new Pair<>(true, auctionId);
    }
    @Override
    public CloseAuctionResponse closeAuction(int auctionId, SealedObject clientRequest) {
        var request = readClientRequest(clientRequest);
        var user = request.getUser();
        if(!auctionItems.containsKey(auctionId))
            return new CloseAuctionResponse(false, false, "N/A");
        var item = auctionItems.get(auctionId);
        if(!Objects.equals(user.getUsername(), item.getSellerName()))
            return new CloseAuctionResponse(true, false, "N/A");
        closeAuctionByItemId(auctionId);
        System.out.println();
        saveFile(Constants.LIST_PATH, auctionItems);
        return new CloseAuctionResponse(true, true, item.getHighestBidName());
    }
    private void closeAuctionByItemId(int auctionId) {
        bids.remove(auctionId);
        saveFile(Constants.BIDS_PATH, bids);
        var item = auctionItems.remove(auctionId);
        System.out.println(Constants.SERVER_NAME +
                " closed the auction with id: " + auctionId);
        int userId = users.stream().filter(
                        x -> Objects.equals(x.getUsername(), item.getHighestBidName()))
                .findFirst().orElse(User.EMPTY).getId();
        if(item.isBid()) {
            System.out.println("The user with the id " + userId
                    + " won the \"" + item.getItemName() + "\" with a bid of " + item.getCurrentBid());
            winners.put(auctionId, item.getHighestBidName());
        }
        else
            System.out.println("Nobody bid for the \"" + item.getItemName() + "\"");
    }
    private ClientRequest readClientRequest(SealedObject clientRequest) {
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
        } catch (ClassNotFoundException e) {
            System.out.println("ERROR:\t couldn't recreate the list");
            throw new RuntimeException(e);
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
}
