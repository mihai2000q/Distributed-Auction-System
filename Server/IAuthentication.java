import javax.crypto.SealedObject;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IAuthentication extends Remote {
    int requestServerChallenge() throws RemoteException;
    boolean sendEncryptedServerChallenge(SealedObject randomNumber) throws RemoteException;
    void sendClientChallenge(int randomNumber) throws RemoteException;
    SealedObject requestEncryptedClientChallenge() throws RemoteException;
}
