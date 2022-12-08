import javax.crypto.SealedObject;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IAuthentication extends Remote {
    int requestServerChallenge(SealedObject clientRequest) throws RemoteException;
    boolean sendEncryptedServerChallenge(SealedObject challenge, SealedObject clientRequest) throws RemoteException;
    boolean sendClientChallenge(int clientChallenge, SealedObject clientRequest) throws RemoteException;
    SealedObject requestEncryptedClientChallenge(SealedObject clientRequest) throws RemoteException;
}
