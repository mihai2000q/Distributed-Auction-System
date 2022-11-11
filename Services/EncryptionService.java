import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.security.*;
import java.security.cert.CertificateException;

public final class EncryptionService implements IEncryptionService{
    public EncryptionService() {
        super();
    }
    @Override
    public SealedObject encryptObject(Object Item, String Algorithm,
                                      String Password, String Alias, String Path) {
        SealedObject result;
        try {
            Cipher cipher = Cipher.getInstance(Algorithm);
            SecureRandom secureRandom = new SecureRandom();
            byte[] aesKey = new byte[16];
            secureRandom.nextBytes(aesKey);
            SecretKey secretKey = new SecretKeySpec(aesKey, Algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            saveKey(secretKey, Password, Alias, Path);
            result = new SealedObject((Serializable) Item, cipher);
        } catch (NoSuchPaddingException | InvalidKeyException | NoSuchAlgorithmException exception) {
            System.out.println("ERROR:\t problem with the key");
            throw new RuntimeException(exception);
        }
        catch (IllegalBlockSizeException | IOException exception) {
            System.out.println("ERROR:\t couldn't create the sealed object");
            throw new RuntimeException(exception);
        }
        return result;
    }
    @Override
    public SecretKey decryptSecretKey(String Password, String Alias, String Path) {
        KeyStore keyStore;
        KeyStore.SecretKeyEntry entry;
        try (FileInputStream inputStream = new FileInputStream(Path)) {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(inputStream, Password.toCharArray());
            KeyStore.ProtectionParameter protectionParam = new KeyStore.PasswordProtection(Password.toCharArray());
            entry = (KeyStore.SecretKeyEntry) keyStore.getEntry(Alias, protectionParam);
        } catch (IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException |
                 UnrecoverableEntryException exception) {
            System.out.println("ERROR:\t Problem with decrypting");
            throw new RuntimeException(exception);
        }
        return entry.getSecretKey();
    }
    private void saveKey(SecretKey secretKey, String Password, String Alias, String Path) {
        try {
            java.nio.file.Path directoryPath = java.nio.file.Path.of("../" + Path.split("/")[1]);
            if(!Files.exists(directoryPath))
                Files.createDirectory(directoryPath);
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            //creating the repository basically, not inserting anything yet
            keyStore.load(null, Password.toCharArray());

            KeyStore.ProtectionParameter protectionParam = new KeyStore.PasswordProtection(Password.toCharArray());
            KeyStore.SecretKeyEntry secretKeyEntry = new KeyStore.SecretKeyEntry(secretKey);
            keyStore.setEntry(Alias, secretKeyEntry, protectionParam);
            FileOutputStream fileOutputStream = new FileOutputStream(Path);
            keyStore.store(fileOutputStream, Password.toCharArray());
        }
        catch (KeyStoreException exception) {
            System.out.println("ERROR:\t couldn't get the instance for the Key Store");
            throw new RuntimeException(exception);
        }
        catch (FileNotFoundException exception) {
            System.out.println("ERROR:\t couldn't find the mentioned path or create the file");
            throw new RuntimeException(exception);
        }
        catch (CertificateException | IOException | NoSuchAlgorithmException exception) {
            System.out.println("ERROR: \t couldn't load/set/store the key properly");
            throw new RuntimeException(exception);
        }
    }
}