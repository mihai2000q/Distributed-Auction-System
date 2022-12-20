import org.jgroups.JChannel;
import org.jgroups.Receiver;

public class GroupUtils implements IGroupUtils {
    public JChannel connect(Receiver receiver) {
        String channelName = System.getenv(Constants.CHANNEL_GROUP) == null
                ? Constants.CHANNEL_DEFAULT_GROUP
                : System.getenv(Constants.CHANNEL_GROUP);
        try {
            JChannel channel = new JChannel();
            channel.connect(channelName);
            channel.setReceiver(receiver);
            channel.setDiscardOwnMessages(true);
            System.out.printf("I connected to jgroups channel: %s\n", channelName);
            return channel;
        } catch (Exception e) {
            System.err.printf("I could not connect to jgroups channel: %s\n", channelName);
        }

        return null;
    }
}
