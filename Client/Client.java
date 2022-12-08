import javax.crypto.SealedObject;
import java.io.Console;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public abstract class Client {
    protected static final IEncryptionService encryptionService = new EncryptionService();
    protected static final Scanner scanner = new Scanner(System.in);
    public Client() {
        super();
    }
    private static<T extends IAuthentication> boolean authentify(T server) {
        try {
            final var serverChallenge = server.requestServerChallenge(createSealedRequest(User.EMPTY));
            var check = server.sendEncryptedServerChallenge(encryptionService.encryptObject(
                                    serverChallenge, Constants.ENCRYPTION_ALGORITHM, Constants.PASSWORD,
                                    Constants.AUTHENTICATION_KEY_ALIAS,Constants.AUTHENTICATION_SECRET_KEY_PATH),
                                createSealedRequest(User.EMPTY));
            if(!check) {
                System.out.println("Authentication failed!!!");
                return false;
            }
            else
                System.out.println("Server authenticated Client");

            final int clientChallenge = Constants.generateRandomInt();
            if(!server.sendClientChallenge(clientChallenge, createSealedRequest(User.EMPTY)))
                System.out.println("Couldn't send the client challenge to the server");
            var obj = server.requestEncryptedClientChallenge(createSealedRequest(User.EMPTY));
            int serverResponse = (int) obj.getObject(encryptionService.decryptSecretKey(Constants.PASSWORD,
                    Constants.AUTHENTICATION_KEY_ALIAS, Constants.AUTHENTICATION_SECRET_KEY_PATH));
            if(serverResponse == clientChallenge)
                System.out.println("Client authenticated Server");
            else {
                System.out.println("Client could not authenticate the server!!!");
                return false;
            }
        } catch (RemoteException exception) {
            System.out.println("ERROR:\t problem while authenticating");
            throw new RuntimeException(exception);
        } catch (IOException | ClassNotFoundException | NoSuchAlgorithmException | InvalidKeyException exception) {
            System.out.println("ERROR:\t problem while trying to use the shared key for authentication");
            throw new RuntimeException(exception);
        }
        return true;
    }
    private static<T extends IAuthentification> Pair<User, Boolean> createAccount(T server,
                                                                                  Constants.ClientType clientType) {
        int answer;
        Console console = System.console();
        do {
            System.out.println("\nPress 1 to login or 2 to signup\n");
            answer = Validation.validateInteger(console.readLine());
            System.out.println();
        } while(answer != 1 && answer != 2);
        if(answer == 1)
            return new Pair<>(User.EMPTY, true);
        System.out.print("Please enter a username: ");
        String username = console.readLine();
        System.out.print("and a password: ");
        String password = new String(console.readPassword());
        var sealedDto = encryptionService.encryptObject(new UserDto(username,password, clientType),
                Constants.ENCRYPTION_ALGORITHM, Constants.PASSWORD,
                Constants.USER_SECRET_KEY_ALIAS, Constants.USER_SECRET_KEY_PATH);
        try {
            SealedObject obj = server.createAccount(sealedDto, createSealedRequest(User.EMPTY));
            var user = (User) obj.getObject(encryptionService.decryptSecretKey(Constants.PASSWORD,
                    Constants.USER_SECRET_KEY_ALIAS, Constants.USER_SECRET_KEY_PATH));
            //user.isEmpty() produces strange bug for some reasons
            if(user.getUsername().equals(User.EMPTY.getUsername())) {
                System.out.println("\nThere is already an account with this username");
                return new Pair<>(User.EMPTY, false);
            }
            else
                return new Pair<>(user, true);
        } catch (IOException | ClassNotFoundException |
                NoSuchAlgorithmException | InvalidKeyException exception) {
            System.err.println("ERROR:\t couldn't create the account");
            throw new RuntimeException(exception);
        }
    }
    private static<T extends IAuthentification> User login(T server, Constants.ClientType clientType) {
        final Console console = System.console();
        System.out.print("username: ");
        String username = Normalization.normalizeUsername(scanner.nextLine());
        System.out.print("password: ");
        String password = new String(console.readPassword());
        var userDto = new UserDto(username, password, clientType);
        final User user;
        System.out.println();
        try {
            var response = server.login(encryptionService.encryptObject(userDto,
                            Constants.ENCRYPTION_ALGORITHM, Constants.PASSWORD,
                            Constants.USER_SECRET_KEY_ALIAS, Constants.USER_SECRET_KEY_PATH),
                    createSealedRequest(User.EMPTY));
            user = response.user();
            if(!response.isAuthorized()) {
                System.out.println("You are not authorized to log in");
                return User.EMPTY;
            }
            else
            if(!response.success()) {
                System.out.println("Username or Password incorrect");
                return User.EMPTY;
            }
            else if(response.alreadyLoggedIn()) {
                System.out.println("You are already logged in from another browser");
                return User.EMPTY;
            }
            else
                System.out.println("Logged in successfully");
        } catch (RemoteException exception) {
            System.out.println("ERROR:\t couldn't log in");
            throw new RuntimeException(exception);
        }
        return user;
    }
    private static<T extends IAuthentification> void logout(T server, User user) {
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
                Constants.CLIENT_REQUEST_SECRET_KEY_ALIAS, Constants.CLIENT_REQUEST_SECRET_KEY_PATH);
    }
    @SuppressWarnings("unchecked")
    protected static<T extends IAuthentification> Pair<T, User> connectToServer(Constants.ClientType clientType) {
        final User user;
        final T server;
        try {
            Registry registry = LocateRegistry.getRegistry();
            server = (T) registry.lookup(Constants.SERVER_NAME);
        }
        catch (RemoteException | NotBoundException exception) {
            System.out.println("ERROR:\t problem while trying to connect to server");
            exception.printStackTrace();
            throw new RuntimeException(exception);
        }
        if(!authentify(server))
            return null;
        var response = createAccount(server, clientType);
        if(response.x().isEmpty() && !response.y())
            return null;
        else if(response.x().isEmpty()) {
            user = login(server, clientType);
            if (user.isEmpty())
                return null;
        }
        else {
            user = response.x();
            System.out.println("\nAccount created with success and automatically logged in!");
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> logout(server, user)));
        return new Pair<>(server,user);
    }
}
