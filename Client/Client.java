import javax.crypto.SealedObject;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Client {
    public Client() {
        super();
    }
    protected static SealedObject createSealedRequest(IEncryptionService encryptionService) {
        return encryptionService.encryptObject(new ClientRequest(Constants.generateRandomInt()),
                Constants.ENCRYPTION_ALGORITHM, Constants.PASSWORD,
                Constants.REQUEST_SECRET_KEY_ALIAS, Constants.REQUEST_SECRET_KEY_PATH);
    }
    protected static <T> boolean contains(final T[] Array, final T variable) {
        for(final T element : Array)
            if(variable == element && variable != null)
                return true;
        return false;
    }
    protected static <T extends Remote> T connectToServer(T type) {
        T result;
        try {
            Registry registry = LocateRegistry.getRegistry(Constants.SERVER_HOST);
            result = (T) registry.lookup(Constants.SERVER_NAME);
        }
        catch (RemoteException | NotBoundException exception) {
            System.out.println("ERROR:\t problem while trying to connect to server");
            exception.printStackTrace();
            throw new RuntimeException(exception);
        }
        return result;
    }

}
