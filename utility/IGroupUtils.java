import org.jgroups.JChannel;
import org.jgroups.Receiver;

public interface IGroupUtils {
    JChannel connect(Receiver receiver);
}
