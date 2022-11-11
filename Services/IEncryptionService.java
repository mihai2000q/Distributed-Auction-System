import javax.crypto.SealedObject;
import javax.crypto.SecretKey;

public interface IEncryptionService {
    SealedObject encryptObject(Object Item, String Algorithm, String Password, String Alias, String Path);
    SecretKey decryptSecretKey(String Password, String Alias, String Path);
}
