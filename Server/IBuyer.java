import javax.crypto.SealedObject;
import java.rmi.RemoteException;

public interface IBuyer extends IAuthentification {
    SealedObject getSpec(int auctionId, SealedObject clientRequest) throws RemoteException;
    BidResponse bidItem(BidRequest request, SealedObject clientRequest) throws RemoteException;
    SealedObject getList(SealedObject clientRequest) throws RemoteException;
}
