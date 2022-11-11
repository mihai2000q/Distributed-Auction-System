import javax.crypto.SealedObject;
import java.rmi.RemoteException;

public interface ISeller extends IAuthentification {
    Pair<Boolean, Integer> createAuction(SealedObject sealedItem, SealedObject clientRequest) throws RemoteException;
    Pair<Boolean, String> closeAuction(int auctionId, SealedObject clientRequest) throws RemoteException;
}
