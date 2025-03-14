import javax.crypto.SealedObject;
import java.rmi.RemoteException;

public interface ISeller extends IAuthentification {
    Pair<Boolean, Integer> createAuction(SealedObject sealedItem, SealedObject clientRequest) throws RemoteException;
    CloseAuctionResponse closeAuction(int auctionId, SealedObject clientRequest) throws RemoteException;
    Pair<SealedObject, Integer> getBids(int auctionId, SealedObject clientRequest) throws RemoteException;
}
