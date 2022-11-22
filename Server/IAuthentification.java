import javax.crypto.SealedObject;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IAuthentification extends IAuthentication {
    LoginResponse login(SealedObject userDto, SealedObject clientRequest) throws RemoteException;
    void logout(SealedObject clientRequest) throws RemoteException;
    SealedObject createAccount(SealedObject user, SealedObject clientRequest) throws RemoteException;
}
