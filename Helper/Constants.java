import java.text.NumberFormat;
import java.util.Locale;
import java.util.Random;

public final class Constants {
    private Constants() {}
    public enum ClientType {Seller, Buyer}
    public static final int REGISTRY_PORT = 1099;
    public static final String SERVER_NAME = "Auction Server";
    public static final String PASSWORD = "admin";
    public static final String ENCRYPTION_ALGORITHM = "AES";
    public static final String ITEM_SECRET_KEY_PATH = "../.sharedKeys/ItemSecretKey.ks";
    public static final String ITEM_SECRET_KEY_ALIAS = "ItemSecretKey";
    public static final String LIST_SECRET_KEY_PATH = "../.sharedKeys/ListSecretKey.ks";
    public static final String LIST_SECRET_KEY_ALIAS = "ListSecretKey";
    public static final String REQUEST_SECRET_KEY_PATH = "../.sharedKeys/RequestSecretKey.ks";
    public static final String REQUEST_SECRET_KEY_ALIAS = "RequestSecretKey";
    public static final String CLIENT_REQUEST_SECRET_KEY_PATH = "../.sharedKeys/ClientRequestSecretKey.ks";
    public static final String CLIENT_REQUEST_SECRET_KEY_ALIAS = "ClientRequestSecretKey";
    public static final String AUTHENTICATION_SECRET_KEY_PATH = "../.sharedKeys/AuthenticationSecretKey.ks";
    public static final String AUTHENTICATION_KEY_ALIAS = "AuthenticationSecretKey";
    public static final String USER_SECRET_KEY_PATH = "../.sharedKeys/UserSecretKey.ks";
    public static final String USER_SECRET_KEY_ALIAS = "UserSecretKey";
    public static final String AUCTION_LIST_PATH = "Data/Auction-list.tmp";
    public static final String BIDS_PATH = "Data/bids.tmp";
    public static final String USERS_PATH = "Data/users.tmp";
    public static final int LIST_CAPACITY = 1000;
    public static final int USERS_CAPACITY = 100;
    public static final NumberFormat FORMATTER = NumberFormat.getCurrencyInstance();
    public static final int DISPATCHER_TIMEOUT = 1000;
    public static int generateRandomInt(){
        return new Random().nextInt((int) Math.pow(2, 10));
    }
}