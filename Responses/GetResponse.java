import javax.crypto.SealedObject;
import java.io.Serializable;

public record GetResponse(boolean hasItem, boolean ongoing, SealedObject sealedItem) implements Serializable {
}
