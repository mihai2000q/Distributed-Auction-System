import org.jgroups.JChannel;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.util.RspList;

import javax.crypto.*;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public final class Frontend extends UnicastRemoteObject implements IBuyer, ISeller {
    private final IEncryptionService encryptionService;
    private final IGroupUtils groupUtils;
    private final JChannel channel;
    private final RpcDispatcher dispatcher;
    public Frontend() throws RemoteException {
        super();
        encryptionService = new EncryptionService();
        groupUtils = new GroupUtils();
        channel = groupUtils.connect();
        if (this.channel == null)
            System.exit(1); // error to be printed by the 'connect' function
        this.bind();

        dispatcher = new RpcDispatcher(channel, this);
        dispatcher.setMembershipListener(new MembershipListener());

    }
    public static void main(String[] args) {
        //launching the server
        System.out.println("Frontend Server running...");
        try {
            new Frontend();
        } catch (RemoteException exception) {
            System.err.println("ERROR:\t problem while launching the frontend server");
            throw new RuntimeException(exception);
        }
    }
    private void bind() {
        try {
            Registry registry = LocateRegistry.createRegistry(Constants.REGISTRY_PORT);
            registry.rebind(Constants.SERVER_NAME, this);
            System.out.println("RMI Server ready\n");
        } catch (RemoteException exception) {
            System.err.println("Error\t: problem while trying to launch the server");
            throw new RuntimeException(exception);
        }
    }
    @Override
    public int requestServerChallenge(SealedObject clientRequest) throws RemoteException {
        return readRspResponses(clientRequest, "requestServerChallenge",
                new Object[] { clientRequest }, new Class[] {SealedObject.class});
    }

    @Override
    public boolean sendEncryptedServerChallenge(SealedObject challenge, SealedObject clientRequest) throws RemoteException {
        return readRspResponses(clientRequest, "sendEncryptedServerChallenge",
                new Object[] { challenge, clientRequest }, new Class[] { SealedObject.class, SealedObject.class });
    }

    @Override
    public boolean sendClientChallenge(int clientChallenge, SealedObject clientRequest) throws RemoteException {
        return readRspResponses(clientRequest, "sendClientChallenge",
                new Object[] { clientChallenge, clientRequest }, new Class[] { int.class, SealedObject.class });
    }

    @Override
    public SealedObject requestEncryptedClientChallenge(SealedObject clientRequest) throws RemoteException {
        return readRspResponses(clientRequest, "requestEncryptedClientChallenge",
                new Object[] { clientRequest }, new Class[] { SealedObject.class });
    }

    @Override
    public LoginResponse login(SealedObject userDto, SealedObject clientRequest) throws RemoteException {
        return readRspResponses(clientRequest, "login",
                new Object[] { userDto, clientRequest }, new Class[] { SealedObject.class, SealedObject.class });
    }

    @Override
    public void logout(SealedObject clientRequest) throws RemoteException {
        readRspResponses(clientRequest, "logout",
                new Object[] { clientRequest }, new Class[] { SealedObject.class });
    }

    @Override
    public SealedObject createAccount(SealedObject user, SealedObject clientRequest) throws RemoteException {
        return readRspResponses(clientRequest, "createAccount",
                new Object[] { user, clientRequest }, new Class[] { SealedObject.class, SealedObject.class });
    }

    @Override
    public GetResponse getSpec(int auctionId, SealedObject clientRequest) throws RemoteException {
        return readRspResponses(clientRequest, "getSpec",
                new Object[] { auctionId, clientRequest }, new Class[] { int.class, SealedObject.class });
    }

    @Override
    public BidResponse bidItem(SealedObject request, SealedObject clientRequest) throws RemoteException {
        return readRspResponses(clientRequest, "bidItem",
                new Object[] { request, clientRequest }, new Class[] { SealedObject.class, SealedObject.class });
    }

    @Override
    public SealedObject getList(SealedObject clientRequest) throws RemoteException {
        return readRspResponses(clientRequest, "getList",
                new Object[] { clientRequest }, new Class[] { SealedObject.class });
    }

    @Override
    public InfoResponse getInfoOnAuction(int auctionId, SealedObject clientRequest) throws RemoteException {
        return readRspResponses(clientRequest, "getInfoOnAuction",
                new Object[] { auctionId, clientRequest }, new Class[] { int.class, SealedObject.class });
    }

    @Override
    public Pair<Boolean, Integer> createAuction(SealedObject sealedItem, SealedObject clientRequest) throws RemoteException {
        return readRspResponses(clientRequest, "createAuction",
                new Object[] { sealedItem, clientRequest }, new Class[] { SealedObject.class, SealedObject.class });
    }

    @Override
    public CloseAuctionResponse closeAuction(int auctionId, SealedObject clientRequest) throws RemoteException {
        return readRspResponses(clientRequest, "closeAuction",
                new Object[] { auctionId, clientRequest }, new Class[] { int.class, SealedObject.class });
    }

    @Override
    public Pair<SealedObject, Integer> getBids(int auctionId, SealedObject clientRequest) throws RemoteException {
        return readRspResponses(clientRequest, "getBids",
                new Object[] { auctionId, clientRequest }, new Class[] { int.class, SealedObject.class });
    }
    private<T> T readRspResponses(SealedObject clientRequest, String methodName,
                                  Object[] args, Class[] types) {
        readClientRequest(clientRequest);
        RspList<T> responses;
        try {
            responses = dispatcher.callRemoteMethods(null, methodName, args, types,
                    new RequestOptions(ResponseMode.GET_ALL, Constants.DISPATCHER_TIMEOUT));
            System.out.println("Frontend server received " + responses.size() + " responses");
        } catch (Exception exception) {
            System.err.println("ERROR:\t while calling remote methods");
            throw new RuntimeException(exception);
        }
        if(!responses.isEmpty() && checkIntegrity(responses))
            return responses.getFirst();
        else {
            System.err.println("ERROR:\t responses either wrong or they are not existent");
            throw new RuntimeException();
        }
    }
    private void readClientRequest(SealedObject clientRequest) {
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
    }
    private<T> boolean checkIntegrity(RspList<T> responses) {
        T temp = responses.getFirst();
        for(T element : responses.getResults())
            if(!Objects.equals(temp, element))
                return false;
        return true;
    }
}
