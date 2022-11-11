import javax.crypto.SealedObject;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ISeller extends Remote {
    Pair<Boolean, Integer> createAuction(SealedObject sealedItem, SealedObject clientRequest) throws RemoteException;
    Pair<Boolean, String> closeAuction(int auctionId, SealedObject clientRequest) throws RemoteException;
}
