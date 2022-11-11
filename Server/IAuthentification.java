import javax.crypto.SealedObject;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IAuthentification extends Remote {
    boolean login(User user, SealedObject clientRequest) throws RemoteException;
    void logout(User user ,SealedObject clientRequest) throws RemoteException;
}
