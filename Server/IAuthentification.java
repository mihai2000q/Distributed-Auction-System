import javax.crypto.SealedObject;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IAuthentification extends IAuthentication {
    boolean login(SealedObject clientRequest) throws RemoteException;
    void logout(SealedObject clientRequest) throws RemoteException;
}
