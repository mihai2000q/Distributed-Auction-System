import org.jgroups.JChannel;

public class GroupUtils implements IGroupUtils {
    public JChannel connect() {
        String channelName = System.getenv("GROUP") == null ? "DEFAULT_GROUP" : System.getenv("GROUP");
        try {
            JChannel channel = new JChannel();
            channel.connect(channelName);
            System.out.printf("I connected to jgroups channel: %s\n", channelName);
            channel.setDiscardOwnMessages(true);
            return channel;
        } catch (Exception e) {
            System.err.printf("I could not connect to jgroups channel: %s\n", channelName);
        }

        return null;
    }
}
