import javax.crypto.SealedObject;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

public class Client {
    protected static final IEncryptionService encryptionService = new EncryptionService();
    public Client() {
        super();
    }
    protected static<T extends IAuthentification> Pair<Boolean, User> login(T server, Constants.ClientType clientType) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("username: ");
        String username = scanner.nextLine();
        System.out.print("password: ");
        String password = scanner.nextLine();
        var user = new User(username, password, clientType);
        try {
            if(server.login(user, createSealedRequest()))
                System.out.println("\nLogged in successfully\n");
            else {
                System.out.println("\nUsername or Password incorrect");
                return new Pair<>(false, User.Empty);
            }
        } catch (RemoteException exception) {
            System.out.println("ERROR:\t couldn't log in");
            throw new RuntimeException(exception);
        }
        return new Pair<>(true, user);
    }
    protected static<T extends IAuthentification> void logout(T server, User user) {
        try {
            server.logout(user, createSealedRequest());
        } catch (RemoteException exception) {
            System.out.println("ERROR:\t couldn't logout");
            throw new RuntimeException(exception);
        }
    }
    protected static SealedObject createSealedRequest() {
        return encryptionService.encryptObject(new ClientRequest(Constants.generateRandomInt()),
                Constants.ENCRYPTION_ALGORITHM, Constants.PASSWORD,
                Constants.REQUEST_SECRET_KEY_ALIAS, Constants.REQUEST_SECRET_KEY_PATH);
    }
    protected static<T extends IAuthentification> T connectToServer() {
        final User user;
        final T server;
        try {
            Registry registry = LocateRegistry.getRegistry(Constants.SERVER_HOST);
            server = (T) registry.lookup(Constants.SERVER_NAME);
        }
        catch (RemoteException | NotBoundException exception) {
            System.out.println("ERROR:\t problem while trying to connect to server");
            exception.printStackTrace();
            throw new RuntimeException(exception);
        }
        var response = login(server, Constants.ClientType.Seller);
        if(!response.x())
            return null; //FIX WHEN THE PASSWORD IS WRONG --- ALSO MAKE PASSWORD NOT VISIBLE
        user = response.y();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> logout(server, user)));
        return server;
    }

}
