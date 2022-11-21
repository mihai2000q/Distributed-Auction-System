import javax.crypto.SealedObject;
import java.rmi.RemoteException;

public interface IBuyer extends IAuthentification {
    SealedObject getSpec(int auctionId, SealedObject clientRequest) throws RemoteException;
    BidResponse bidItem(SealedObject request, SealedObject clientRequest) throws RemoteException;
    SealedObject getList(SealedObject clientRequest) throws RemoteException;
    InfoResponse getInfoOnAuction(int auctionId, SealedObject clientRequest) throws RemoteException;
}
