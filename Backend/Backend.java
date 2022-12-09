import org.jgroups.JChannel;
import org.jgroups.blocks.RpcDispatcher;

import javax.crypto.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

public final class Backend {
    private int serverChallengeNumber;
    private int clientChallengeNumber;
    private final Map<Integer, AuctionItem> auctionItems;
    private final Map<Integer, List<Bid>> bids;
    private final List<User> users;
    private final Collection<User> activeUsers;
    private final IEncryptionService encryptionService;
    private final IGroupUtils groupUtils;
    private final JChannel channel;
    private final RpcDispatcher dispatcher;
    private int requestCount;
    public Backend() {
        super();
        encryptionService = new EncryptionService();
        auctionItems = new HashMap<>(Constants.LIST_CAPACITY);
        bids = new HashMap<>();
        users = new ArrayList<>(Constants.USERS_CAPACITY);
        activeUsers = new ArrayList<>();
        loadMapFile(Constants.AUCTION_LIST_PATH, auctionItems);
        loadMapFile(Constants.BIDS_PATH, bids);
        loadListFile(Constants.USERS_PATH, users);

        this.requestCount = 0;
        groupUtils = new GroupUtils();
        channel = groupUtils.connect();
        if(channel == null)
            System.exit(1);

        dispatcher = new RpcDispatcher(channel, this);
    }
    public static void main(String[] args) {
        //launching the server
        new Backend();
    }
    
    public int requestServerChallenge(SealedObject clientRequest) {
        readClientRequest(clientRequest);
        serverChallengeNumber = Constants.generateRandomInt();
        System.out.println("Server sent authentication challenge for client");
        return serverChallengeNumber;
    }

    public boolean sendEncryptedServerChallenge(SealedObject challenge, SealedObject clientRequest) {
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
            System.err.println("ERROR:\t problem while decrypting the random number");
            throw new RuntimeException(exception);
        }
    }

    public boolean sendClientChallenge(int clientChallenge, SealedObject clientRequest) {
        readClientRequest(clientRequest);
        clientChallengeNumber = clientChallenge;
        System.out.println("Server received the client challenge");
        return true;
    }

    public SealedObject requestEncryptedClientChallenge(SealedObject clientRequest) {
        readClientRequest(clientRequest);
        System.out.println("Server got authenticated by client");
        return encryptionService.encryptObject(clientChallengeNumber,
                Constants.ENCRYPTION_ALGORITHM, Constants.PASSWORD,
                Constants.AUTHENTICATION_KEY_ALIAS, Constants.AUTHENTICATION_SECRET_KEY_PATH);
    }

    public LoginResponse login(SealedObject UserDto, SealedObject clientRequest) {
        readClientRequest(clientRequest);
        UserDto userDto;
        try {
            userDto = (UserDto) UserDto.getObject(encryptionService.decryptSecretKey(Constants.PASSWORD,
                    Constants.USER_SECRET_KEY_ALIAS, Constants.USER_SECRET_KEY_PATH));
        } catch (IOException | ClassNotFoundException |
                 NoSuchAlgorithmException | InvalidKeyException exception) {
            System.err.println("ERROR:\t couldn't decrypt the user dto while logging in");
            throw new RuntimeException(exception);
        }
        User userToCompare = new User(Constants.generateRandomInt(), userDto.username(),
                userDto.password(), userDto.clientType());
        var query = users.stream().filter(
                x -> x.getUsername().equals(Normalization.normalizeString(userDto.username()))).toList();
        var result = query.size() > 0 ? query.get(0) : User.EMPTY;
        if(!result.isEmpty() && !result.getClientType().equals(userDto.clientType()))
            return new LoginResponse(User.EMPTY, false,true, false);
        if(!users.contains(userToCompare)) {
            System.out.println("Failed login attempt");
            return new LoginResponse(User.EMPTY, true,true, false);
        }
        User user = users.get(users.indexOf(userToCompare));
        if(activeUsers.contains(user)) {
            System.out.println("Failed login attempt");
            return new LoginResponse(user, true, true, false);
        }
        activeUsers.add(user);
        System.out.println(user.getUsername() + " logged in\n");
        System.out.println("Number of active users is " + activeUsers.size());
        return new LoginResponse(user, true,false, true);
    }

    public boolean logout(SealedObject clientRequest) {
        var user = readClientRequest(clientRequest).getUser();
        activeUsers.remove(user);
        System.out.println(user.getUsername() + " logged out\n");
        System.out.println("Number of active users is " + activeUsers.size());
        return true;
    }

    public SealedObject createAccount(SealedObject user, SealedObject clientRequest) {
        readClientRequest(clientRequest);
        UserDto userDto;
        try {
            userDto = (UserDto) user.getObject(encryptionService.decryptSecretKey(Constants.PASSWORD,
                    Constants.USER_SECRET_KEY_ALIAS, Constants.USER_SECRET_KEY_PATH));
        } catch (IOException | ClassNotFoundException |
                 NoSuchAlgorithmException | InvalidKeyException exception) {
            System.err.println("ERROR:\t couldn't decrypt the user while creating the account");
            throw new RuntimeException(exception);
        }
        if(userDto.username().equals(User.EMPTY.getUsername())) {
            System.out.println("Failed attempt to create account");
            return encryptionService.encryptObject(User.EMPTY, Constants.ENCRYPTION_ALGORITHM, Constants.PASSWORD,
                    Constants.USER_SECRET_KEY_ALIAS, Constants.USER_SECRET_KEY_PATH);
        }
        User theUser = new User(Constants.generateRandomInt(),
                userDto.username(), userDto.password(), userDto.clientType());
        var query = users.stream().filter(
                x -> x.getUsername().equals(Normalization.normalizeString(userDto.username()))).toList();
        if(query.size() > 0) {
            System.out.println("Failed attempt to recreate the same account");
            return encryptionService.encryptObject(User.EMPTY, Constants.ENCRYPTION_ALGORITHM, Constants.PASSWORD,
                    Constants.USER_SECRET_KEY_ALIAS, Constants.USER_SECRET_KEY_PATH);
        }
        users.add(theUser);
        saveFile(Constants.USERS_PATH, users);
        System.out.println("Successfully created another account with the username " + theUser.getUsername());
        activeUsers.add(theUser);
        System.out.println("\nNumber of active users is " + activeUsers.size());
        return encryptionService.encryptObject(theUser, Constants.ENCRYPTION_ALGORITHM, Constants.PASSWORD,
                Constants.USER_SECRET_KEY_ALIAS, Constants.USER_SECRET_KEY_PATH);
    }

    public GetResponse getSpecByItemId(int auctionId, SealedObject clientRequest) {
        readClientRequest(clientRequest);
        var item = getSpecByItemId(auctionId);
        if(item.isEmpty())
            return new GetResponse(false, false,
                    encryptionService.encryptObject(AuctionItem.EMPTY, Constants.ENCRYPTION_ALGORITHM,
                            Constants.PASSWORD, Constants.ITEM_SECRET_KEY_ALIAS, Constants.ITEM_SECRET_KEY_PATH));
        if(!item.isOngoing())
            return new GetResponse(true, false,
                    encryptionService.encryptObject(AuctionItem.EMPTY, Constants.ENCRYPTION_ALGORITHM,
                    Constants.PASSWORD, Constants.ITEM_SECRET_KEY_ALIAS, Constants.ITEM_SECRET_KEY_PATH));
        System.out.println("Sent details on auction: " + auctionId);
        var sealedObject = encryptionService.encryptObject(item, Constants.ENCRYPTION_ALGORITHM,
                Constants.PASSWORD, Constants.ITEM_SECRET_KEY_ALIAS, Constants.ITEM_SECRET_KEY_PATH);
        return new GetResponse(true, true, sealedObject);
    }

    public BidResponse bidItem(SealedObject BidRequest, SealedObject clientRequest) {
        var request = readClientRequest(clientRequest);
        var user = request.getUser();
        BidRequest bidRequest;
        try {
            bidRequest = (BidRequest) BidRequest.getObject(encryptionService.decryptSecretKey(
                    Constants.PASSWORD, Constants.REQUEST_SECRET_KEY_ALIAS, Constants.REQUEST_SECRET_KEY_PATH));
        } catch (IOException | ClassNotFoundException |
                 NoSuchAlgorithmException | InvalidKeyException exception) {
            System.err.println("ERROR:\t couldn't decrypt the key while bidding");
            throw new RuntimeException(exception);
        }
        var auctionId = bidRequest.auctionId();
        if(!auctionItems.containsKey(auctionId)) {
            System.out.println("Failed attempt to bid");
            return new BidResponse(false, true, false, -1);
        }
        var item = auctionItems.get(auctionId);
        if(!item.isOngoing()) {
            System.out.println("Failed attempt to bid on auction that has closed");
            return new BidResponse(true, true, false, -1);
        }
        if(bidRequest.bid() < item.getReservePrice()) {
            System.out.println(user.getUsername() +
                    " hasn't reached the reserved price of " + Constants.FORMATTER.format(item.getReservePrice()));
            return new BidResponse(true, true, true, -1);
        }
        if(bidRequest.bid() <= item.getCurrentBid()) {
            System.out.println("Bid failed as it was too low!!!");
            return new BidResponse(true, false, true, -1);
        }

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

        if (result.isEmpty()) {
            System.out.println(user.getUsername() +
                    " made its first bid on \"" + item.getItemName() + "\"");
            bids.get(auctionId).add(new Bid(Constants.generateRandomInt(), item.getItemName(),
                                            user.getUsername(), bidRequest.bid()));
            saveFile(Constants.BIDS_PATH, bids);
        }
        else {
            if(bidRequest.bid() <= result.getBid()) {
                System.out.println("Failed attempt to lower bid");
                return new BidResponse(true, false, true, 0);
            }
            System.out.println(user.getUsername() +
                    " changed its bid for \"" + item.getItemName() + "\" from " +
                    Constants.FORMATTER.format(result.getBid()) + " to " +
                    Constants.FORMATTER.format(bidRequest.bid()));
            bids.get(auctionId).remove(result);
            bids.get(auctionId).add(new Bid(result.getId(), result.getItemName(),
                    result.getUsername(), bidRequest.bid()));
            saveFile(Constants.BIDS_PATH, bids);
        }

        if(bidRequest.bid() >= item.getCurrentBid())
            item.setNewBid(user.getUsername(), bidRequest.bid());
        return new BidResponse(true, false, true, 1);

    }

    public SealedObject getList(SealedObject clientRequest) {
        readClientRequest(clientRequest);
        System.out.println("Sent the whole list");
        return encryptionService.encryptObject(
                getAllOngoingItems().stream().sorted(Comparator.reverseOrder()).toList(),
                Constants.ENCRYPTION_ALGORITHM, Constants.PASSWORD,
                Constants.LIST_SECRET_KEY_ALIAS, Constants.LIST_SECRET_KEY_PATH);
    }

    public InfoResponse getInfoOnAuction(int auctionId, SealedObject clientRequest) {
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

    public Pair<Boolean, Integer> createAuction(SealedObject sealedItem, SealedObject clientRequest)
            {
        var secretKey = encryptionService.decryptSecretKey(Constants.PASSWORD,
                Constants.ITEM_SECRET_KEY_ALIAS, Constants.ITEM_SECRET_KEY_PATH);
        final AuctionItem item;
        AuctionItemDto itemDto;
        int itemId = Constants.generateRandomInt();

        //in case there is an item with the same ID already, compute another one
        for(AuctionItem element : auctionItems.values())
            while(element.getId() == itemId)
                itemId = Constants.generateRandomInt();

        var request = readClientRequest(clientRequest);
        var user = request.getUser();
        try {
            itemDto = (AuctionItemDto) sealedItem.getObject(secretKey);
        } catch (IOException | ClassNotFoundException |
                 NoSuchAlgorithmException | InvalidKeyException exception) {
            System.err.println("ERROR:\t couldn't unlock the sealed item");
            throw new RuntimeException(exception);
        }
        //compute an auctionId
        int auctionId = Constants.generateRandomInt();
        for(Integer element : auctionItems.keySet())
            while(element == auctionId)
                auctionId = Constants.generateRandomInt();
        item = new AuctionItem(itemId, auctionId,
                itemDto.itemName(), itemDto.sellerName(),
                itemDto.reservePrice(), itemDto.startingPrice(), itemDto.description());
        if(auctionItems.containsValue(item)) {
            System.out.println("Failed attempt to add the same item");
            return new Pair<>(false, -1);
        }
        auctionItems.put(auctionId,item);
        saveFile(Constants.AUCTION_LIST_PATH, auctionItems);
        System.out.println(Constants.SERVER_NAME + " received item with id: " + item.getId() + "\n");
        System.out.println(user.getUsername() + " created auction with the id: " + auctionId);
        bids.put(auctionId, new ArrayList<>());
        saveFile(Constants.BIDS_PATH, bids);
        return new Pair<>(true, auctionId);
    }

    public CloseAuctionResponse closeAuction(int auctionId, SealedObject clientRequest) {
        var request = readClientRequest(clientRequest);
        var user = request.getUser();
        if(!auctionItems.containsKey(auctionId)) {
            System.out.println("Failed attempt to close auction");
            return new CloseAuctionResponse(false, false, true, "N/A");
        }
        var item = auctionItems.get(auctionId);
        if(!Objects.equals(user.getUsername(), item.getSellerName())) {
            System.out.println("Failed attempt to close auction");
            return new CloseAuctionResponse(true, false, true, "N/A");
        }
        if(!item.isOngoing()) {
            System.out.println("Failed attempt to close auction");
            return new CloseAuctionResponse(true, true, true, "N/A");
        }
        closeAuctionByItemId(auctionId);
        return new CloseAuctionResponse(true, true, false, item.getHighestBidName());
    }

    public Pair<SealedObject, Integer> getBids(int auctionId, SealedObject clientRequest) {
        var user = readClientRequest(clientRequest).getUser();
        var item = auctionItems.getOrDefault(auctionId, AuctionItem.EMPTY);
        if(!auctionItems.containsKey(auctionId)) {
            System.out.println("Failed attempt to get bids");
            return new Pair<>(null, -1);
        }
        if(!user.getUsername().equals(item.getSellerName())) {
            System.out.println("Failed attempt to get bids");
            return new Pair<>(null, 0);
        }
        var list = bids.getOrDefault(auctionId, new ArrayList<>())
                                .stream().sorted(Comparator.reverseOrder()).toList();
        System.out.println("Sent bids for auction: " + auctionId);
        return new Pair<>(encryptionService.encryptObject(list,
                Constants.ENCRYPTION_ALGORITHM, Constants.PASSWORD,
                Constants.LIST_SECRET_KEY_ALIAS, Constants.LIST_SECRET_KEY_PATH), 1);
    }
    private AuctionItem getSpecByItemId(int auctionId){
        return auctionItems.getOrDefault(auctionId, AuctionItem.EMPTY);
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
            if(user.isEmpty()) {
                System.err.println("ERROR:\t there should be a winner if the item is bid");
                throw new RuntimeException();
            }
            System.out.println("The user with the id " + user.getId()
                    + " won the \"" + item.getItemName() + "\" with a bid of " +
                    Constants.FORMATTER.format(item.getCurrentBid()));
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
            System.err.println("ERROR:\t couldn't unlock the client request");
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
            System.err.println("ERROR:\t problem while trying to read the file");
            throw new RuntimeException(exception);
        } catch (ClassNotFoundException exception) {
            System.err.println("ERROR:\t couldn't recreate the list");
            throw new RuntimeException(exception);
        }
    }
    private <T> void loadListFile(String path, List<T> list) {
        if(!Files.exists(Path.of(path)))
            return;
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(path));
            @SuppressWarnings("unchecked")
            var outputList = (List<T>) objectInputStream.readObject();
            if(outputList != null)
                list.addAll(outputList);
            objectInputStream.close();
        } catch (IOException exception) {
            System.err.println("ERROR:\t problem while trying to read the file");
            throw new RuntimeException(exception);
        } catch (ClassNotFoundException exception) {
            System.err.println("ERROR:\t couldn't recreate the list");
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
            System.err.println("ERROR:\t problem while creating the file");
            throw new RuntimeException(exception);
        }
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
