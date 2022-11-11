import javax.crypto.SealedObject;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IServer extends Remote {
    SealedObject getSpec(int auctionId, SealedObject clientRequest) throws RemoteException;
    boolean bidItem(BidRequest request, SealedObject clientRequest) throws RemoteException;
    Pair<Boolean, Integer> createAuction(SealedObject sealedItem, SealedObject clientRequest) throws RemoteException;
    Pair<Boolean, String> closeAuction(int auctionId, SealedObject clientRequest) throws RemoteException;
    SealedObject getList(SealedObject clientRequest) throws RemoteException;
}
