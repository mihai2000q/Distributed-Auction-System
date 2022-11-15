import javax.crypto.SealedObject;
import java.io.Console;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

public abstract class Client {
    protected static final IEncryptionService encryptionService = new EncryptionService();
    public Client() {
        super();
    }
    protected static<T extends IAuthentification> Pair<Boolean, User> login(T server, Constants.ClientType clientType) {
        final Scanner scanner = new Scanner(System.in);
        final Console console = System.console();
        System.out.print("username: ");
        String username = Normalization.normalizeUsername(scanner.nextLine());
        System.out.print("password: ");
        String password = new String(console.readPassword());
        var user = new User(-1, username, password, clientType);
        try {
            if(server.login(createSealedRequest(user)))
                System.out.println("\nLogged in successfully\n");
            else {
                System.out.println("\nUsername or Password incorrect");
                return new Pair<>(false, User.EMPTY);
            }
        } catch (RemoteException exception) {
            System.out.println("ERROR:\t couldn't log in");
            throw new RuntimeException(exception);
        }
        return new Pair<>(true, user);
    }
    protected static<T extends IAuthentification> void logout(T server, User user) {
        try {
            server.logout(createSealedRequest(user));
        } catch (RemoteException exception) {
            System.out.println("ERROR:\t couldn't logout");
            throw new RuntimeException(exception);
        }
    }
    protected static SealedObject createSealedRequest(User user) {
        return encryptionService.encryptObject(new ClientRequest(Constants.generateRandomInt(), user),
                Constants.ENCRYPTION_ALGORITHM, Constants.PASSWORD,
                Constants.REQUEST_SECRET_KEY_ALIAS, Constants.REQUEST_SECRET_KEY_PATH);
    }
    @SuppressWarnings("unchecked")
    protected static<T extends IAuthentification> Pair<T, User> connectToServer(Constants.ClientType clientType) {
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
        var response = login(server, clientType);
        if(!response.x())
            return null;
        user = response.y();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> logout(server, user)));
        System.out.println("\n----------------------\n");
        return new Pair<>(server,user);
    }
}
