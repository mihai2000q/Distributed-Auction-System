import java.util.Random;

public final class Constants {
    private Constants() {}
    public enum ClientType {Seller, Buyer}
    public static final String SERVER_NAME = "Auction Server";
    public static final String SERVER_HOST = "localhost";
    public static final short SERVER_PORT = 0;
    public static final String PASSWORD = "admin";
    public static final String ENCRYPTION_ALGORITHM = "AES";
    public static final String ITEM_SECRET_KEY_PATH = "../.sharedKeys/ItemSecretKey.ks";
    public static final String ITEM_SECRET_KEY_ALIAS = "ItemSecretKey";
    public static final String LIST_SECRET_KEY_PATH = "../.sharedKeys/ListSecretKey.ks";
    public static final String LIST_SECRET_KEY_ALIAS = "ListSecretKey";
    public static final String REQUEST_SECRET_KEY_PATH = "../.sharedKeys/RequestSecretKey.ks";
    public static final String REQUEST_SECRET_KEY_ALIAS = "RequestSecretKey";
    public static final String LIST_PATH = "Data/list.tmp";
    public static final String BIDS_PATH = "Data/bids.tmp";
    public static final int LIST_CAPACITY = 1000;
    public static final int USERS_CAPACITY = 10;
    public static int generateRandomInt(){
        return new Random().nextInt((int) Math.pow(2, 10));
    }
}